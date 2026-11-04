package com.example.problemfinder.domain.workspaces.impl

import com.example.problemfinder.core.model.workspace.*
import com.example.problemfinder.data.workspaces.api.WorkspaceRepository
import com.example.problemfinder.domain.workspaces.api.WorkspaceWorkflows
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

class WorkspaceWorkflowsImpl @Inject constructor(private val repository: WorkspaceRepository) : WorkspaceWorkflows {
    private fun identity(id: String) { require(id.isNotBlank() && id.length <= 200) }
    override fun observeProjects() = repository.observeProjects()
    override fun observeAnalysis(projectId: String) = repository.observeAnalysis(projectId.also(::identity))
    override fun observeBuild(projectId: String) = repository.observeBuild(projectId.also(::identity))
    override suspend fun refreshProjects() = repository.refreshProjects()
    override suspend fun refreshAnalysis(projectId: String) = repository.refreshAnalysis(projectId.also(::identity))
    override suspend fun generateAnalysis(projectId: String) = repository.generateAnalysis(projectId.also(::identity))
    override suspend fun confirmThesis(projectId: String) =
        repository.updateAnalysis(projectId.also(::identity), "thesis", "thesis", "confirmed", true)
    override suspend fun pinProblem(projectId: String, problemId: String, pinned: Boolean) =
        repository.updateAnalysis(projectId.also(::identity), "problem_unit", problemId.also(::identity), "pinned", pinned)
    override suspend fun rejectProblem(projectId: String, problemId: String, rejected: Boolean) =
        repository.updateAnalysis(projectId.also(::identity), "problem_unit", problemId.also(::identity), "rejected", rejected)
    override suspend fun resolveAssumption(projectId: String, assumptionId: String, resolved: Boolean) =
        repository.updateAnalysis(projectId.also(::identity), "assumption", assumptionId.also(::identity), "resolved", resolved)
    override suspend fun listConversations(projectId: String) = repository.listConversations(projectId.also(::identity))
    override fun observeConversationList(projectId: String) = repository.observeConversationList(projectId.also(::identity))
    override fun observeConversation(projectId: String, conversationId: String) =
        repository.observeConversation(projectId.also(::identity), conversationId.also(::identity))
    override suspend fun createConversation(projectId: String) = repository.createConversation(projectId.also(::identity))
    override suspend fun loadConversation(projectId: String, conversationId: String) =
        repository.loadConversation(projectId.also(::identity), conversationId.also(::identity))
    override suspend fun sendMessage(projectId: String, conversationId: String?, message: String): AnalysisConversation {
        identity(projectId)
        require(message.trim().length in 1..4000)
        return repository.sendMessage(projectId, conversationId, message.trim())
    }
    override suspend fun decideProposal(projectId: String, conversationId: String, proposalId: String, accept: Boolean) =
        repository.decideProposal(projectId.also(::identity), conversationId.also(::identity), proposalId.also(::identity), accept)
    override suspend fun refreshBuild(projectId: String) = repository.refreshBuild(projectId.also(::identity))
    override suspend fun generateBuild(projectId: String, request: BuildGeneration) =
        repository.generateBuild(projectId.also(::identity), request)
    override suspend fun approveBuild(projectId: String, expectedVersion: Int) {
        identity(projectId); require(expectedVersion > 0)
        repository.approveBuild(projectId, expectedVersion)
    }
    override suspend fun updateTask(projectId: String, taskId: String, expectedVersion: Int, update: TaskUpdate) {
        identity(projectId); identity(taskId); require(expectedVersion > 0)
        repository.updateTask(projectId, taskId, expectedVersion, update)
    }
    override suspend fun saveDocument(projectId: String, key: String, expectedVersion: Int, content: String): SolutionCase {
        identity(projectId); identity(key); require(expectedVersion > 0)
        require(content.isNotBlank() && content.length <= 120_000)
        return repository.saveDocument(projectId, key, expectedVersion, content)
    }
    override suspend fun readDocumentDraft(projectId: String, key: String) = repository.readDocumentDraft(projectId.also(::identity), key.also(::identity))
    override suspend fun saveDocumentDraft(userId: String, projectId: String, draft: DocumentDraft) {
        identity(userId); identity(projectId); identity(draft.key)
        require(draft.baseVersion > 0 && draft.content.length <= 120_000)
        repository.saveDocumentDraft(userId, projectId, draft)
    }
    override suspend fun clearDocumentDraft(userId: String, projectId: String, key: String) =
        repository.clearDocumentDraft(userId.also(::identity), projectId.also(::identity), key.also(::identity))
    override suspend fun exportBuild(projectId: String) = repository.exportBuild(projectId.also(::identity))
}

@Module
@InstallIn(SingletonComponent::class)
abstract class WorkspaceDomainModule {
    @Binds @Singleton abstract fun workflows(implementation: WorkspaceWorkflowsImpl): WorkspaceWorkflows
}
