package com.example.problemfinder.feature.projects.impl

import androidx.lifecycle.SavedStateHandle
import com.example.problemfinder.core.designsystem.components.FeatureContentStatus
import com.example.problemfinder.core.model.workspace.*
import com.example.problemfinder.core.mvi.BaseMVIViewModel
import com.example.problemfinder.domain.workspaces.api.WorkspaceWorkflows
import com.example.problemfinder.feature.projects.api.ProjectsDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import javax.inject.Inject

@HiltViewModel
class AnalysisWorkspaceViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val workflows: WorkspaceWorkflows,
) : BaseMVIViewModel<AnalysisIntent, AnalysisWorkspaceState, AnalysisEffect>(
    AnalysisWorkspaceState(projectId = savedState[ProjectsDestination.PROJECT_ID])
) {
    private val projectId = savedState.get<String>(ProjectsDestination.PROJECT_ID).orEmpty()
    private var observer: Job? = null
    private var refreshJob: Job? = null
    private var mutationJob: Job? = null
    private var pollJob: Job? = null
    private var conversationListJob: Job? = null
    private var conversationObserver: Job? = null
    private var conversationLoad: Job? = null
    private var conversationListRefresh: Job? = null
    private var sendJob: Job? = null
    private var generation = 0L
    private var conversationGeneration = 0L
    private var needsRefresh = true
    private var panel = AnalysisPanel.OVERVIEW
    private var problemId: String? = null
    private var audienceId: String? = null
    private var latest: AnalysisWorkspaceData? = null
    private val visibleOwners = mutableSetOf<String>()
    private var newConversationChosen = false

    override fun onIntent(intent: AnalysisIntent) {
        when (intent) {
            is AnalysisIntent.Entered -> {
                panel = intent.panel
                if (panel == AnalysisPanel.PROBLEM) problemId = intent.itemId
                if (panel == AnalysisPanel.AUDIENCE) audienceId = intent.itemId
                if (observer?.isActive != true) observe()
                else { publish(); if (panel == AnalysisPanel.CONVERSATION) startConversations() }
            }
            is AnalysisIntent.VisibilityChanged -> {
                if (intent.visible) visibleOwners.add(intent.owner) else visibleOwners.remove(intent.owner)
                if (visibleOwners.isEmpty()) { pollJob?.cancel(); pollJob = null } else pollIfNeeded()
            }
            AnalysisIntent.Back -> sendEffect(AnalysisEffect.Back)
            AnalysisIntent.Reload -> if (observer?.isActive != true) observe() else {
                reload(); if (panel == AnalysisPanel.CONVERSATION) refreshConversations()
            }
            AnalysisIntent.Generate -> mutate(needsCase = false) { workflows.generateAnalysis(projectId) }
            AnalysisIntent.ConfirmThesis -> if (latest?.analysis?.thesis?.confirmed == false) mutate {
                workflows.confirmThesis(projectId)
            }
            is AnalysisIntent.PinProblem -> latest?.analysis?.problems?.find { it.id == intent.id }?.let { problem ->
                mutate { workflows.pinProblem(projectId, problem.id, !problem.pinned) }
            }
            is AnalysisIntent.RejectProblem -> latest?.analysis?.problems?.find { it.id == intent.id }?.let { problem ->
                mutate { workflows.rejectProblem(projectId, problem.id, !problem.rejected) }
            }
            is AnalysisIntent.ResolveAssumption -> if (latest?.analysis?.assumptions?.any { it.id == intent.id } == true)
                mutate { workflows.resolveAssumption(projectId, intent.id, intent.resolved) }
            is AnalysisIntent.OpenProblem -> sendEffect(AnalysisEffect.OpenProblem(intent.id))
            is AnalysisIntent.OpenAudience -> sendEffect(AnalysisEffect.OpenAudience(intent.id))
            AnalysisIntent.OpenAssumptions -> sendEffect(AnalysisEffect.OpenAssumptions)
            AnalysisIntent.OpenConversation -> sendEffect(AnalysisEffect.OpenConversation)
            is AnalysisIntent.OpenOpportunity -> sendEffect(AnalysisEffect.OpenOpportunity(intent.clusterId))
            is AnalysisIntent.OpenPost -> sendEffect(AnalysisEffect.OpenPost(intent.clusterId, intent.postId))
            AnalysisIntent.OpenBuild -> if ((latest?.analysis?.version ?: 0) > 0) sendEffect(AnalysisEffect.OpenBuild)
            is AnalysisIntent.MessageChanged -> {
                reduce { copy(draftMessage = intent.value) }
                if (state.value.userId != null) savedState["message"] = intent.value
            }
            is AnalysisIntent.ConversationSelected -> if (!state.value.isSending) selectConversation(intent.id)
            AnalysisIntent.NewConversation -> if (!state.value.isSending) {
                newConversationChosen = true; conversationGeneration++
                conversationObserver?.cancel(); conversationLoad?.cancel()
                savedState["conversationId"] = null
                reduce { copy(conversationId = null, turns = emptyList(), conversationFailure = null, isLoadingConversation = false) }
            }
            AnalysisIntent.RetryConversation -> refreshConversations()
            AnalysisIntent.SendMessage -> sendMessage()
            is AnalysisIntent.DecideProposal -> decide(intent)
        }
    }

    private fun current(ticket: Long, owner: String) = ticket == generation && state.value.userId == owner
    private fun failure(error: Throwable) = (error as? WorkspaceException)?.failure ?: WorkspaceFailure.UNKNOWN
    private fun cancelOperations() {
        generation++
        listOf(refreshJob, mutationJob, pollJob, conversationListJob, conversationObserver,
            conversationLoad, conversationListRefresh, sendJob).forEach { it?.cancel() }
        pollJob = null; conversationListJob = null; conversationObserver = null
        conversationGeneration++
    }
    private fun observe() {
        if (projectId.isBlank()) return onError(WorkspaceException(WorkspaceFailure.NOT_FOUND))
        needsRefresh = true
        observer = safeLaunch {
            workflows.observeAnalysis(projectId).collect { snapshot ->
                if (state.value.userId != snapshot.userId) {
                    cancelOperations(); latest = null; needsRefresh = true
                    val sameSavedOwner = savedState.get<String>("owner") == snapshot.userId
                    if (!sameSavedOwner) {
                        savedState["conversationId"] = null; savedState["message"] = ""
                    }
                    savedState["owner"] = snapshot.userId
                    reduce { AnalysisWorkspaceState(projectId = projectId, userId = snapshot.userId,
                        conversationId = savedState["conversationId"], draftMessage = savedState.get<String>("message").orEmpty()) }
                }
                if (snapshot.cacheReady && snapshot.cacheFailure == null) latest = snapshot.value
                reduce { copy(cacheReady = snapshot.cacheReady, failure = snapshot.cacheFailure ?: failure) }
                publish()
                if (snapshot.cacheReady && snapshot.userId != null && needsRefresh) {
                    needsRefresh = false; reload()
                }
                if (panel == AnalysisPanel.CONVERSATION && snapshot.cacheReady) startConversations()
                pollIfNeeded()
            }
        }
    }
    private fun publish() {
        val data = latest
        val analysis = data?.analysis
        val workspace = if (data != null && analysis != null && analysis.version > 0) {
            val evidence = analysis.evidence.associateBy { it.id.toString() }
            AnalysisWorkspace(data.project.id, data.project.clusterId, data.project.name,
                analysis.thesis?.statement.orEmpty(), analysis.version.toString(), analysis.thesis?.statement.orEmpty(),
                analysis.thesis?.consequence.orEmpty(), analysis.thesis?.workaround.orEmpty(),
                analysis.problems.map { AnalysisProblem(it.id, it.title, it.description.orEmpty(), it.kind,
                    it.affectedUsers, it.situation.orEmpty(), it.evidenceIds.mapNotNull(evidence::get), it.pinned, it.rejected) },
                analysis.audiences.map { AnalysisAudience(it.id, it.name, it.description, it.language, it.communities, it.reachChannels, it.unknowns) },
                analysis.assumptions.map { AnalysisAssumption(it.id, it.question, it.whyItMatters, it.resolutionEvidence, it.resolved) },
                analysis.thesis?.confirmed == true)
        } else null
        reduce { copy(
            workspace = workspace, selectedProblem = workspace?.problems?.find { it.id == problemId },
            selectedAudience = workspace?.audiences?.find { it.id == audienceId },
            version = analysis?.version ?: 0, caseStatus = analysis?.status ?: WorkspaceStatus.NOT_STARTED,
            progressLabel = analysis?.progress.orEmpty(), safeError = analysis?.safeError, claims = analysis?.claims.orEmpty(),
            status = when {
                failure != null -> FeatureContentStatus.ERROR
                !cacheReady || analysis?.status?.isGenerating == true || (isRefreshing && workspace == null) -> FeatureContentStatus.LOADING
                workspace == null -> FeatureContentStatus.EMPTY
                else -> FeatureContentStatus.CONTENT
            },
        ) }
    }
    private fun reload() {
        val owner = state.value.userId ?: return
        if (!state.value.cacheReady || refreshJob?.isActive == true || mutationJob?.isActive == true) return
        val ticket = generation
        reduce { copy(isRefreshing = true, failure = null) }; publish()
        refreshJob = safeLaunch {
            try { workflows.refreshAnalysis(projectId) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { if (current(ticket, owner)) { pollJob?.cancel(); reduce { copy(failure = failure(error)) } } }
            finally { if (current(ticket, owner)) { reduce { copy(isRefreshing = false) }; publish() } }
        }
    }
    private fun mutate(needsCase: Boolean = true, block: suspend () -> Unit) {
        val owner = state.value.userId ?: return
        if (!state.value.cacheReady || mutationJob?.isActive == true || refreshJob?.isActive == true ||
            state.value.caseStatus?.isGenerating == true || (needsCase && state.value.workspace == null)) return
        val ticket = generation
        reduce { copy(isMutating = true, failure = null) }
        mutationJob = safeLaunch {
            try { block() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { if (current(ticket, owner)) reduce { copy(failure = failure(error)) } }
            finally { if (current(ticket, owner)) { reduce { copy(isMutating = false) }; publish() } }
        }
    }
    private fun pollIfNeeded() {
        if (visibleOwners.isEmpty() || state.value.caseStatus?.isGenerating != true ||
            state.value.failure != null || pollJob?.isActive == true) return
        val owner = state.value.userId ?: return
        val ticket = generation
        pollJob = safeLaunch {
            try {
                while (current(ticket, owner) && visibleOwners.isNotEmpty() && state.value.caseStatus?.isGenerating == true) {
                    delay(2_000)
                    if (!current(ticket, owner) || visibleOwners.isEmpty() || state.value.caseStatus?.isGenerating != true) break
                    workflows.refreshAnalysis(projectId)
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { if (current(ticket, owner)) { reduce { copy(failure = failure(error)) }; publish() } }
        }
    }

    private fun startConversations() {
        if (state.value.userId == null || state.value.workspace == null || conversationListJob?.isActive == true) return
        val owner = state.value.userId ?: return
        val ticket = generation
        conversationListJob = safeLaunch {
            try {
                workflows.observeConversationList(projectId).collect { snapshot ->
                    if (!current(ticket, owner) || snapshot.userId != owner) return@collect
                    reduce { copy(conversations = snapshot.value.orEmpty(), conversationFailure = snapshot.cacheFailure ?: conversationFailure) }
                    val selected = state.value.conversationId
                    if (selected != null && conversationObserver?.isActive != true) selectConversation(selected)
                    else if (selected == null && !newConversationChosen && !snapshot.value.isNullOrEmpty())
                        snapshot.value?.firstOrNull()?.let { selectConversation(it.id) }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { if (current(ticket, owner)) reduce { copy(conversationFailure = failure(error)) } }
        }
        refreshConversations()
    }
    private fun refreshConversations() {
        if (state.value.workspace == null || conversationListRefresh?.isActive == true) return
        val owner = state.value.userId ?: return
        val ticket = generation
        reduce { copy(isLoadingConversation = true, conversationFailure = null) }
        conversationListRefresh = safeLaunch {
            try {
                workflows.listConversations(projectId)
                state.value.conversationId?.let { workflows.loadConversation(projectId, it) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { if (current(ticket, owner)) reduce { copy(conversationFailure = failure(error)) } }
            finally { if (current(ticket, owner)) reduce { copy(isLoadingConversation = false) } }
        }
    }
    private fun selectConversation(id: String, refresh: Boolean = true) {
        val owner = state.value.userId ?: return
        if (state.value.conversationId == id && conversationObserver?.isActive == true) return
        conversationGeneration++
        val selectedTicket = conversationGeneration
        val ticket = generation
        conversationObserver?.cancel(); conversationLoad?.cancel()
        newConversationChosen = false
        savedState["conversationId"] = id
        reduce { copy(conversationId = id, turns = emptyList(), conversationFailure = null) }
        conversationObserver = safeLaunch {
            try {
                workflows.observeConversation(projectId, id).collect { snapshot ->
                    if (!current(ticket, owner) || selectedTicket != conversationGeneration || snapshot.userId != owner) return@collect
                    snapshot.value?.let { conversation -> reduce { copy(conversationFailure = null, turns = conversation.turns.map {
                        AnalysisTurn(it.id, it.text, it.isUser, it.citations, it.proposal, it.insufficientEvidence)
                    }) } }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { if (current(ticket, owner) && selectedTicket == conversationGeneration)
                reduce { copy(conversationFailure = failure(error)) } }
        }
        if (refresh) {
            reduce { copy(isLoadingConversation = true) }
            conversationLoad = safeLaunch {
                try { workflows.loadConversation(projectId, id) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (error: Exception) { if (current(ticket, owner) && selectedTicket == conversationGeneration)
                    reduce { copy(conversationFailure = failure(error)) } }
                finally { if (current(ticket, owner) && selectedTicket == conversationGeneration) reduce { copy(isLoadingConversation = false) } }
            }
        }
    }
    private fun sendMessage() {
        val owner = state.value.userId ?: return
        val text = state.value.draftMessage.trim()
        if (text.length !in 1..4000) { reduce { copy(conversationFailure = WorkspaceFailure.REJECTED) }; return }
        if (sendJob?.isActive == true || state.value.workspace == null) return
        val ticket = generation
        reduce { copy(isSending = true, conversationFailure = null) }
        sendJob = safeLaunch {
            try {
                val id = state.value.conversationId ?: workflows.createConversation(projectId).id.also {
                    if (current(ticket, owner)) selectConversation(it, refresh = false)
                }
                if (!current(ticket, owner)) return@safeLaunch
                workflows.sendMessage(projectId, id, text)
                if (current(ticket, owner) && state.value.draftMessage.trim() == text) {
                    savedState["message"] = ""; reduce { copy(draftMessage = "") }
                }
                workflows.listConversations(projectId)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { if (current(ticket, owner)) reduce { copy(conversationFailure = failure(error)) } }
            finally { if (current(ticket, owner)) reduce { copy(isSending = false) } }
        }
    }
    private fun decide(intent: AnalysisIntent.DecideProposal) {
        val owner = state.value.userId ?: return
        val id = state.value.conversationId ?: return
        if (sendJob?.isActive == true || state.value.turns.none { it.proposal?.let { p -> p.id == intent.id && p.status == "pending" } == true }) return
        val ticket = generation
        reduce { copy(isSending = true, conversationFailure = null) }
        sendJob = safeLaunch {
            try { workflows.decideProposal(projectId, id, intent.id, intent.accept) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { if (current(ticket, owner)) reduce { copy(conversationFailure = failure(error)) } }
            finally { if (current(ticket, owner)) reduce { copy(isSending = false) } }
        }
    }
    override fun onError(error: Throwable) {
        cancelOperations()
        reduce { copy(failure = failure(error), isRefreshing = false, isMutating = false,
            isSending = false, isLoadingConversation = false, status = FeatureContentStatus.ERROR) }
    }
}
