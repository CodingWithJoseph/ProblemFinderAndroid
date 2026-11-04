package com.example.problemfinder.data.workspaces.impl

import com.example.problemfinder.core.model.workspace.*
import com.example.problemfinder.data.authentication.api.AuthSession
import com.example.problemfinder.data.authentication.api.AuthSessionProvider
import com.example.problemfinder.data.workspaces.api.WorkspaceRepository
import io.ktor.client.plugins.*
import io.ktor.serialization.ContentConvertException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

/** One writer per account/project. Network validation completes before replacing a saved document. */
internal class WorkspaceRepositoryImpl @Inject constructor(
    private val sessions: AuthSessionProvider,
    private val remote: WorkspaceRemoteDataSource,
    private val dao: WorkspaceCacheDao,
    private val json: Json,
) : WorkspaceRepository {
    private val locks = ConcurrentHashMap<Pair<String, String>, Mutex>()

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun <T> observe(key: String, decode: (String, String) -> T): Flow<WorkspaceSnapshot<T>> =
        sessions.observeUserId().distinctUntilChanged().flatMapLatest { owner ->
            if (owner == null) flowOf(WorkspaceSnapshot<T>(null, cacheReady = true))
            else dao.observe(owner, key).distinctUntilChanged().map { row ->
                try { WorkspaceSnapshot(owner, row?.let { validated { decode(it.payload, owner) } }, cacheReady = true) }
                catch (_: IllegalArgumentException) {
                    WorkspaceSnapshot<T>(owner, cacheReady = true, cacheFailure = WorkspaceFailure.INVALID_RESPONSE)
                } catch (_: WorkspaceException) {
                    // An obsolete/corrupt derived cache is a completed read, allowing explicit HTTP
                    // refresh to repair it. It must never become successful empty domain content.
                    WorkspaceSnapshot<T>(owner, cacheReady = true, cacheFailure = WorkspaceFailure.INVALID_RESPONSE)
                }
            }.flowOn(Dispatchers.Default).onStart { emit(WorkspaceSnapshot(owner)) }
        }

    override fun observeProjects() = observe("projects") { value, owner ->
        json.decodeFromString<ProjectsDto>(value).data.map { it.toModel(owner) }
    }
    override fun observeAnalysis(projectId: String) = observe("analysis:" + projectId) { value, owner ->
        json.decodeFromString<AnalysisCacheDto>(value).toModel(owner)
    }
    override fun observeBuild(projectId: String) = observe("build:" + projectId) { value, owner ->
        json.decodeFromString<BuildCacheDto>(value).toModel(owner)
    }
    override fun observeConversationList(projectId: String) = observe("conversations:" + projectId) { value, _ ->
        json.decodeFromString<List<ConversationSummaryDto>>(value).map { ConversationSummary(it.id, it.title, it.archived) }
    }
    override fun observeConversation(projectId: String, conversationId: String) =
        observe("conversation:" + projectId + ":" + conversationId) { value, _ ->
            json.decodeFromString<ConversationDto>(value).toModel()
        }

    private suspend fun sameAccount(owner: String) {
        if (sessions.observeUserId().first() != owner) throw CancellationException("Workspace account changed")
    }
    private suspend fun <T> request(key: String, block: suspend (AuthSession) -> T): T = workspaceCall {
        withContext(Dispatchers.IO) {
        val session = sessions.currentSession() ?: throw WorkspaceException(WorkspaceFailure.SESSION)
        locks.getOrPut(session.userId to key) { Mutex() }.withLock {
            sameAccount(session.userId)
            val result = block(session)
            sameAccount(session.userId)
            result
        }
        }
    }
    private suspend fun save(owner: String, key: String, payload: String) {
        sameAccount(owner)
        dao.put(WorkspaceCacheEntity(owner, key, payload))
    }
    override suspend fun refreshProjects() = request("projects") { session ->
        val result = remote.projects(session.accessToken)
        validated {
            require(result.data.map { it.id }.distinct().size == result.data.size)
            result.data.forEach { it.toModel(session.userId) }
        }
        save(session.userId, "projects", json.encodeToString(result))
    }
    private suspend fun saveAnalysis(session: AuthSession, id: String, project: ProjectDto, document: AnalysisDto?) {
        require(project.id == id)
        val record = AnalysisCacheDto(project, document)
        validated { record.toModel(session.userId) }
        save(session.userId, "analysis:" + id, json.encodeToString(record))
    }
    override suspend fun refreshAnalysis(projectId: String) = request(projectId) { session ->
        val project = remote.project(projectId, session.accessToken)
        // A missing case after a successful owned-project lookup means analysis has not started.
        val document = try { remote.analysis(projectId, session.accessToken) }
            catch (error: ClientRequestException) { if (error.response.status.value == 404) null else throw error }
        saveAnalysis(session, projectId, project, document)
    }
    override suspend fun generateAnalysis(projectId: String) = request(projectId) { session ->
        val project = remote.project(projectId, session.accessToken)
        val result = remote.generateAnalysis(projectId, session.accessToken)
        saveAnalysis(session, projectId, project, result)
    }
    override suspend fun updateAnalysis(projectId: String, kind: String, objectId: String, property: String, value: Boolean) =
        request(projectId) { session ->
            val project = remote.project(projectId, session.accessToken)
            val result = remote.updateAnalysis(projectId, kind, objectId, property, value, session.accessToken)
            saveAnalysis(session, projectId, project, result)
        }
    private suspend fun saveConversation(session: AuthSession, projectId: String, value: ConversationDto): AnalysisConversation {
        val model = validated { value.toModel() }
        save(session.userId, "conversation:" + projectId + ":" + value.id, json.encodeToString(value))
        return model
    }
    override suspend fun listConversations(projectId: String) = request(projectId) { session ->
        val summaries = remote.conversations(projectId, session.accessToken)
        validated {
            require(summaries.all { it.id.isNotBlank() && it.title.isNotBlank() })
            require(summaries.distinctBy { it.id }.size == summaries.size)
        }
        save(session.userId, "conversations:" + projectId, json.encodeToString(summaries))
        summaries.map { ConversationSummary(it.id, it.title, it.archived) }
    }
    override suspend fun createConversation(projectId: String) = request(projectId) { session ->
        saveConversation(session, projectId, remote.createConversation(projectId, session.accessToken))
    }
    override suspend fun loadConversation(projectId: String, conversationId: String) = request(projectId) { session ->
        val document = remote.conversation(projectId, conversationId, session.accessToken)
        require(document.id == conversationId)
        saveConversation(session, projectId, document)
    }
    override suspend fun sendMessage(projectId: String, conversationId: String?, message: String) = request(projectId) { session ->
        val selected = conversationId ?: remote.createConversation(projectId, session.accessToken).also {
            saveConversation(session, projectId, it)
        }.id
        val document = remote.message(projectId, selected, message, session.accessToken)
        require(document.id == selected)
        saveConversation(session, projectId, document)
    }
    override suspend fun decideProposal(projectId: String, conversationId: String, proposalId: String, accept: Boolean) =
        request(projectId) { session ->
            val document = remote.decide(projectId, conversationId, proposalId, accept, session.accessToken)
            require(document.id == conversationId)
            val result = saveConversation(session, projectId, document)
            if (accept) {
                val project = remote.project(projectId, session.accessToken)
                saveAnalysis(session, projectId, project, remote.analysis(projectId, session.accessToken))
            }
            result
        }
    private suspend fun saveBuild(session: AuthSession, id: String, project: ProjectDto, document: SolutionDto) {
        require(project.id == id)
        val record = BuildCacheDto(project, document)
        validated { record.toModel(session.userId) }
        save(session.userId, "build:" + id, json.encodeToString(record))
    }
    override suspend fun refreshBuild(projectId: String) = request(projectId) { session ->
        val project = remote.project(projectId, session.accessToken)
        saveBuild(session, projectId, project, remote.solution(projectId, session.accessToken))
    }
    override suspend fun generateBuild(projectId: String, request: BuildGeneration) = request(projectId) { session ->
        val project = remote.project(projectId, session.accessToken)
        saveBuild(session, projectId, project, remote.generateBuild(projectId, request, session.accessToken))
    }
    override suspend fun approveBuild(projectId: String, expectedVersion: Int) = request(projectId) { session ->
        val project = remote.project(projectId, session.accessToken)
        val approved = remote.approve(projectId, expectedVersion, session.accessToken)
        require(approved.project.pipelineId == projectId && approved.version == expectedVersion &&
            approved.approvalStatus == "approved" && approved.approvedVersion == expectedVersion)
        // Approval must retain task progress, which is overlaid by GET rather than the approval response.
        saveBuild(session, projectId, project, remote.solution(projectId, session.accessToken))
    }
    override suspend fun updateTask(projectId: String, taskId: String, expectedVersion: Int, update: TaskUpdate) =
        request(projectId) { session ->
            val project = remote.project(projectId, session.accessToken)
            val result = remote.updateTask(projectId, taskId, expectedVersion, update, session.accessToken)
            require(result.taskId == taskId && result.status == update.status.name.lowercase())
            saveBuild(session, projectId, project, remote.solution(projectId, session.accessToken))
        }
    override suspend fun saveDocument(projectId: String, key: String, expectedVersion: Int, content: String) =
        request(projectId) { session ->
            val project = remote.project(projectId, session.accessToken)
            val document = remote.saveDocument(projectId, key, expectedVersion, content, session.accessToken)
            require(project.id == projectId && document.project.pipelineId == projectId && document.version > expectedVersion &&
                document.documents.any { it.key == key })
            val record = BuildCacheDto(project, document)
            val model = validated { record.toModel(session.userId) }
            sameAccount(session.userId)
            dao.saveEditedDocument(WorkspaceCacheEntity(session.userId, "build:" + projectId, json.encodeToString(record)),
                "draft:" + projectId + ":" + key)
            model.solution
        }
    override suspend fun readDocumentDraft(projectId: String, key: String): DocumentDraft? {
        val owner = sessions.observeUserId().first() ?: return null
        val saved = dao.read(owner, "draft:" + projectId + ":" + key) ?: return null
        sameAccount(owner)
        val draft = json.decodeFromString<DraftDto>(saved.payload)
        require(draft.key == key && draft.baseVersion > 0)
        return DocumentDraft(key, draft.baseVersion, draft.content)
    }
    override suspend fun saveDocumentDraft(userId: String, projectId: String, draft: DocumentDraft) {
        save(userId, "draft:" + projectId + ":" + draft.key, json.encodeToString(DraftDto(draft.key, draft.baseVersion, draft.content)))
    }
    override suspend fun clearDocumentDraft(userId: String, projectId: String, key: String) {
        sameAccount(userId)
        dao.delete(userId, "draft:" + projectId + ":" + key)
    }
    override suspend fun exportBuild(projectId: String) = request(projectId) { session ->
        remote.export(projectId, session.accessToken)
    }
}

internal inline fun <T> validated(block: () -> T): T = try { block() }
catch (error: IllegalArgumentException) { throw WorkspaceException(WorkspaceFailure.INVALID_RESPONSE, error) }

internal suspend fun <T> workspaceCall(block: suspend () -> T): T = try { block() }
catch (error: CancellationException) { throw error }
catch (error: WorkspaceException) { throw error }
catch (error: Exception) {
    val failure = when (error) {
        is ResponseException -> when (error.response.status.value) {
            401, 403 -> WorkspaceFailure.SESSION
            404 -> WorkspaceFailure.NOT_FOUND
            409 -> WorkspaceFailure.CONFLICT
            in 400..499 -> WorkspaceFailure.REJECTED
            else -> WorkspaceFailure.SERVER
        }
        is SerializationException, is ContentConvertException, is IllegalArgumentException -> WorkspaceFailure.INVALID_RESPONSE
        is IOException, is HttpRequestTimeoutException -> WorkspaceFailure.NETWORK
        else -> WorkspaceFailure.UNKNOWN
    }
    throw WorkspaceException(failure, error)
}
