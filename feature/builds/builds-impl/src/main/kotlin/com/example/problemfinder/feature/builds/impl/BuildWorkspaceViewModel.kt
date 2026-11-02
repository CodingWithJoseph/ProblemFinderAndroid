package com.example.problemfinder.feature.builds.impl

import androidx.lifecycle.SavedStateHandle
import com.example.problemfinder.core.designsystem.components.FeatureContentStatus
import com.example.problemfinder.core.model.workspace.*
import com.example.problemfinder.core.mvi.BaseMVIViewModel
import com.example.problemfinder.domain.workspaces.api.WorkspaceWorkflows
import com.example.problemfinder.feature.builds.api.BuildsDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import javax.inject.Inject

@HiltViewModel
class BuildWorkspaceViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val workflows: WorkspaceWorkflows,
) : BaseMVIViewModel<BuildIntent, BuildWorkspaceState, BuildEffect>(
    BuildWorkspaceState(projectId = savedState[BuildsDestination.PROJECT_ID])
) {
    private val projectId = savedState.get<String>(BuildsDestination.PROJECT_ID).orEmpty()
    private var observer: Job? = null
    private var refreshJob: Job? = null
    private var mutationJob: Job? = null
    private var pollJob: Job? = null
    private var draftJob: Job? = null
    private var draftRead: Job? = null
    private var exportJob: Job? = null
    private var generation = 0L
    private var needsRefresh = true
    private var latest: BuildWorkspaceData? = null
    private var featureId: String? = null
    private var taskId: String? = null
    private var documentId: String? = null
    private var loadedDraftKey: String? = null
    private var acknowledgedDocument: DocumentDraft? = null
    private var inputsRestored = false
    private var pendingTaskVersion: Int? = null
    private var leaveAfterDiscard = true
    private val visibleOwners = mutableSetOf<String>()

    override fun onIntent(intent: BuildIntent) {
        when (intent) {
            is BuildIntent.Entered -> {
                when (intent.panel) {
                    BuildPanel.FEATURE -> featureId = intent.itemId
                    BuildPanel.TASK -> taskId = intent.itemId
                    BuildPanel.DOCUMENT -> if (documentId != intent.itemId) {
                        documentId = intent.itemId; loadedDraftKey = null
                        reduce { copy(selectedDocument = null, documentDraft = "", documentDirty = false, documentBaseVersion = null) }
                    }
                    else -> Unit
                }
                if (observer?.isActive != true) observe() else { publish(); restoreDraft() }
            }
            is BuildIntent.VisibilityChanged -> {
                if (intent.visible) visibleOwners.add(intent.owner) else visibleOwners.remove(intent.owner)
                if (visibleOwners.isEmpty()) { pollJob?.cancel(); pollJob = null } else pollIfNeeded()
            }
            BuildIntent.Back -> {
                if (state.value.isSaving) return
                if (state.value.documentDirty) { leaveAfterDiscard = true; reduce { copy(confirmLeave = true) } }
                else sendEffect(BuildEffect.Back)
            }
            BuildIntent.KeepEditing -> reduce { copy(confirmLeave = false) }
            BuildIntent.DiscardDocumentChanges -> { leaveAfterDiscard = false; reduce { copy(confirmLeave = true) } }
            BuildIntent.DiscardAndLeave -> discardDraft(leaveAfterDiscard)
            BuildIntent.SaveAndLeave -> saveDocument(leave = leaveAfterDiscard)
            BuildIntent.Reload -> if (observer?.isActive != true) observe() else reload()
            BuildIntent.Generate -> generate()
            BuildIntent.Approve -> state.value.workspace?.takeIf { !it.approved }?.let { workspace ->
                mutate { workflows.approveBuild(projectId, workspace.version) }
            }
            is BuildIntent.OpenFeature -> sendEffect(BuildEffect.OpenFeature(intent.id))
            is BuildIntent.OpenTask -> sendEffect(BuildEffect.OpenTask(intent.id))
            BuildIntent.OpenPlan -> sendEffect(BuildEffect.OpenPlan)
            BuildIntent.OpenDocuments -> sendEffect(BuildEffect.OpenDocuments)
            is BuildIntent.OpenDocument -> sendEffect(BuildEffect.OpenDocument(intent.id))
            BuildIntent.OpenAnalysis -> sendEffect(BuildEffect.OpenAnalysis)
            is BuildIntent.OpenOpportunity -> sendEffect(BuildEffect.OpenOpportunity(intent.clusterId))
            is BuildIntent.TargetSelected -> {
                val targets = state.value.deliveryTargets
                val updated = if (intent.target in targets) targets - intent.target else targets + intent.target
                if (updated.isNotEmpty() && state.value.caseStatus?.isGenerating != true) {
                    reduce { copy(deliveryTargets = updated) }; saveInputs()
                }
            }
            is BuildIntent.ConstraintsChanged -> { reduce { copy(constraintsInput = intent.value) }; saveInputs() }
            is BuildIntent.StackChanged -> { reduce { copy(preferredStackInput = intent.value) }; saveInputs() }
            is BuildIntent.TaskStatusSelected -> selectTaskStatus(intent.status)
            is BuildIntent.TaskNoteChanged -> reduce { copy(taskNote = intent.value) }
            is BuildIntent.VerificationSummaryChanged -> reduce { copy(verificationSummary = intent.value) }
            is BuildIntent.VerificationChecksChanged -> reduce { copy(verificationChecks = intent.value) }
            BuildIntent.ConfirmTaskStatus -> confirmTask()
            BuildIntent.CancelTaskStatus -> reduce { copy(pendingTaskStatus = null) }
            is BuildIntent.DocumentChanged -> changeDocument(intent.value)
            BuildIntent.SaveDocument -> saveDocument()
            BuildIntent.Export -> export()
            BuildIntent.ExportFailed -> reduce { copy(failure = WorkspaceFailure.UNKNOWN) }
        }
    }
    private fun failure(error: Throwable) = (error as? WorkspaceException)?.failure ?: WorkspaceFailure.UNKNOWN
    private fun current(ticket: Long, owner: String) = ticket == generation && state.value.userId == owner
    private fun cancelOperations() {
        generation++
        listOf(refreshJob, mutationJob, pollJob, draftJob, draftRead, exportJob).forEach { it?.cancel() }
        pollJob = null
    }
    private fun saveInputs() {
        if (state.value.userId == null) return
        savedState["targets"] = state.value.deliveryTargets.map { it.name }.toTypedArray()
        savedState["constraints"] = state.value.constraintsInput
        savedState["stack"] = state.value.preferredStackInput
    }
    private fun observe() {
        if (projectId.isBlank()) return onError(WorkspaceException(WorkspaceFailure.NOT_FOUND))
        needsRefresh = true
        observer = safeLaunch {
            workflows.observeBuild(projectId).collect { snapshot ->
                if (state.value.userId != snapshot.userId) {
                    cancelOperations(); latest = null; needsRefresh = true; loadedDraftKey = null
                    acknowledgedDocument = null; inputsRestored = false
                    val sameSavedOwner = savedState.get<String>("owner") == snapshot.userId
                    if (!sameSavedOwner) {
                        savedState.remove<Array<String>>("targets"); savedState.remove<String>("constraints"); savedState.remove<String>("stack")
                    }
                    savedState["owner"] = snapshot.userId
                    val targets = savedState.get<Array<String>>("targets")?.mapNotNull { name ->
                        DeliveryTarget.entries.firstOrNull { it.name == name }
                    }?.toSet()?.takeIf { it.isNotEmpty() } ?: setOf(DeliveryTarget.WEB)
                    reduce { BuildWorkspaceState(projectId = projectId, userId = snapshot.userId,
                        deliveryTargets = targets, constraintsInput = savedState.get<String>("constraints").orEmpty(),
                        preferredStackInput = savedState.get<String>("stack").orEmpty()) }
                }
                if (snapshot.cacheReady && snapshot.cacheFailure == null) latest = snapshot.value
                if (!inputsRestored && snapshot.value != null) {
                    val model = snapshot.value?.solution
                    if (savedState.get<Array<String>>("targets") == null && model != null) reduce {
                        copy(deliveryTargets = model.targets,
                            constraintsInput = if (savedState.contains("constraints")) constraintsInput else model.constraints.joinToString("\n"))
                    }
                    inputsRestored = true; saveInputs()
                }
                reduce { copy(cacheReady = snapshot.cacheReady, failure = snapshot.cacheFailure ?: failure) }
                publish(); restoreDraft()
                if (snapshot.cacheReady && snapshot.userId != null && needsRefresh) { needsRefresh = false; reload() }
                pollIfNeeded()
            }
        }
    }
    private fun publish() {
        val model = latest?.solution
        val workspace = latest?.takeIf { it.solution.version > 0 }?.toPresentation()
        val cachedDocument = workspace?.documents?.find { it.id == documentId }
        // Save completion can arrive before Room's invalidation emission. Keep the edit buffer's
        // acknowledged version/content until the saved snapshot catches up, without regressing it.
        val acknowledged = acknowledgedDocument?.takeIf { it.key == documentId && (workspace?.version ?: 0) < it.baseVersion }
        if (acknowledgedDocument != null && (workspace?.version ?: 0) >= acknowledgedDocument!!.baseVersion) acknowledgedDocument = null
        val document = if (acknowledged != null) cachedDocument?.copy(content = acknowledged.content) else cachedDocument
        reduce { copy(
            workspace = workspace, selectedFeature = workspace?.features?.find { it.id == featureId },
            selectedTask = workspace?.tasks?.find { it.id == taskId }, selectedDocument = document,
            documentDraft = if (documentDirty) documentDraft else document?.content.orEmpty(),
            documentBaseVersion = if (documentDirty) documentBaseVersion else acknowledged?.baseVersion ?: workspace?.version,
            caseStatus = model?.status ?: WorkspaceStatus.NOT_STARTED,
            progressLabel = model?.progress.orEmpty(), safeError = model?.safeError,
            status = when {
                failure != null -> FeatureContentStatus.ERROR
                !cacheReady || model?.status?.isGenerating == true || (isRefreshing && workspace == null) -> FeatureContentStatus.LOADING
                workspace == null -> FeatureContentStatus.EMPTY
                else -> FeatureContentStatus.CONTENT
            },
        ) }
    }
    private fun reload() {
        val owner = state.value.userId ?: return
        if (!state.value.cacheReady || refreshJob?.isActive == true || mutationJob?.isActive == true) return
        val ticket = generation
        reduce { copy(isRefreshing = true, failure = null, pendingTaskStatus = null) }; publish()
        refreshJob = safeLaunch {
            try { workflows.refreshBuild(projectId) }
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
            finally { if (current(ticket, owner)) { reduce { copy(isMutating = false, isSaving = false) }; publish() } }
        }
    }
    private fun generate() {
        val request = try {
            BuildGeneration(state.value.deliveryTargets,
                state.value.constraintsInput.lines().map(String::trim).filter(String::isNotEmpty),
                state.value.preferredStackInput.lines().map(String::trim).filter(String::isNotEmpty))
        } catch (_: IllegalArgumentException) { reduce { copy(failure = WorkspaceFailure.REJECTED) }; return }
        if (state.value.documentDirty) { reduce { copy(failure = WorkspaceFailure.CONFLICT) }; return }
        mutate(needsCase = false) { workflows.generateBuild(projectId, request) }
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
                    workflows.refreshBuild(projectId)
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { if (current(ticket, owner)) { reduce { copy(failure = failure(error)) }; publish() } }
        }
    }
    private fun selectTaskStatus(status: BuildTaskStatus) {
        val task = state.value.selectedTask ?: return
        val workspace = state.value.workspace ?: return
        if (!workspace.approved) { reduce { copy(failure = WorkspaceFailure.CONFLICT) }; return }
        if (state.value.isMutating || state.value.caseStatus?.isGenerating == true || task.status == status) return
        pendingTaskVersion = workspace.version
        reduce { copy(pendingTaskStatus = status, taskNote = "", verificationSummary = "", verificationChecks = "", failure = null) }
        if (status == BuildTaskStatus.TODO || status == BuildTaskStatus.IN_PROGRESS) confirmTask()
    }
    private fun confirmTask() {
        val state = state.value
        val task = state.selectedTask ?: return
        val status = state.pendingTaskStatus ?: return
        val version = pendingTaskVersion ?: return
        val request = try {
            TaskUpdate(TaskProgress.valueOf(status.name), state.taskNote.trim().takeIf(String::isNotEmpty),
                state.verificationSummary.trim().takeIf(String::isNotEmpty),
                state.verificationChecks.lines().map(String::trim).filter(String::isNotEmpty).map { TaskCheck(it) })
        } catch (_: IllegalArgumentException) { reduce { copy(failure = WorkspaceFailure.REJECTED) }; return }
        val ticket = generation
        val owner = state.userId ?: return
        mutate {
            workflows.updateTask(projectId, task.id, version, request)
            if (current(ticket, owner)) reduce { copy(pendingTaskStatus = null) }
        }
    }
    private fun restoreDraft() {
        val owner = state.value.userId ?: return
        val document = state.value.selectedDocument ?: return
        if (!document.editable || loadedDraftKey == document.id || !state.value.cacheReady) return
        loadedDraftKey = document.id
        draftRead?.cancel()
        val ticket = generation
        draftRead = safeLaunch {
            try {
                val draft = workflows.readDocumentDraft(projectId, document.id)
                if (current(ticket, owner) && documentId == document.id && !state.value.documentDirty && draft != null && acknowledgedDocument?.key != document.id) reduce {
                    copy(documentDraft = draft.content, documentBaseVersion = if (draft.content == selectedDocument?.content) workspace?.version else draft.baseVersion,
                        documentDirty = draft.content != selectedDocument?.content)
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { if (current(ticket, owner)) reduce { copy(failure = failure(error)) } }
        }
    }
    private fun changeDocument(value: String) {
        val owner = state.value.userId ?: return
        val document = state.value.selectedDocument ?: return
        if (!document.editable || state.value.isSaving) return
        if (value.length > 120_000) { reduce { copy(failure = WorkspaceFailure.REJECTED) }; return }
        val version = state.value.documentBaseVersion ?: return
        draftRead?.cancel()
        reduce { copy(documentDraft = value, documentDirty = value != document.content) }
        draftJob?.cancel()
        val ticket = generation
        draftJob = safeLaunch {
            delay(300)
            try {
                if (current(ticket, owner)) workflows.saveDocumentDraft(owner, projectId, DocumentDraft(document.id, version, value))
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { if (current(ticket, owner)) reduce { copy(failure = failure(error)) } }
        }
    }
    private fun saveDocument(leave: Boolean = false) {
        val state = state.value
        val document = state.selectedDocument ?: run { reduce { copy(failure = WorkspaceFailure.CONFLICT) }; return }
        val version = state.documentBaseVersion ?: return
        val owner = state.userId ?: return
        if (state.documentDraft.isBlank() || state.documentDraft.length > 120_000) {
            reduce { copy(failure = WorkspaceFailure.REJECTED) }; return
        }
        if (!state.documentDirty || state.isMutating || state.isRefreshing || state.caseStatus?.isGenerating == true) return
        draftJob?.cancel(); draftRead?.cancel()
        val ticket = generation
        reduce { copy(isSaving = true) }
        mutate {
            val saved = workflows.saveDocument(projectId, document.id, version, state.documentDraft)
            if (current(ticket, owner)) {
                val content = saved.documents.find { it.key == document.id }?.content ?: state.documentDraft
                acknowledgedDocument = DocumentDraft(document.id, saved.version, content)
                reduce { copy(documentDraft = content, documentBaseVersion = saved.version, documentDirty = false, confirmLeave = false) }
                if (leave) sendEffect(BuildEffect.Back)
            }
        }
    }
    private fun discardDraft(leave: Boolean) {
        val owner = state.value.userId ?: return
        val key = state.value.selectedDocument?.id ?: documentId ?: return
        if (mutationJob?.isActive == true) return
        draftJob?.cancel(); draftRead?.cancel()
        val ticket = generation
        mutationJob = safeLaunch {
            try {
                workflows.clearDocumentDraft(owner, projectId, key)
                if (current(ticket, owner)) {
                    reduce { copy(documentDirty = false, documentDraft = selectedDocument?.content.orEmpty(),
                        documentBaseVersion = workspace?.version, confirmLeave = false, failure = null) }
                    if (leave) sendEffect(BuildEffect.Back)
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { if (current(ticket, owner)) reduce { copy(failure = failure(error)) } }
        }
    }
    private fun export() {
        val owner = state.value.userId ?: return
        if (exportJob?.isActive == true || state.value.workspace == null || state.value.caseStatus?.isGenerating == true) return
        val ticket = generation
        reduce { copy(isExporting = true, failure = null) }
        exportJob = safeLaunch {
            try {
                val result = workflows.exportBuild(projectId)
                if (current(ticket, owner)) sendEffect(BuildEffect.ExportReady(owner, result))
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { if (current(ticket, owner)) reduce { copy(failure = failure(error)) } }
            finally { if (current(ticket, owner)) reduce { copy(isExporting = false) } }
        }
    }
    override fun onError(error: Throwable) {
        cancelOperations()
        reduce { copy(failure = failure(error), isRefreshing = false, isMutating = false,
            isSaving = false, isExporting = false, status = FeatureContentStatus.ERROR) }
    }
}
