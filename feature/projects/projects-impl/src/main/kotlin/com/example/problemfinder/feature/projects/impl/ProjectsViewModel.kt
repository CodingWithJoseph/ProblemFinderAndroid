package com.example.problemfinder.feature.projects.impl

import com.example.problemfinder.core.designsystem.components.FeatureContentStatus
import com.example.problemfinder.core.model.workspace.*
import com.example.problemfinder.core.mvi.BaseMVIViewModel
import com.example.problemfinder.domain.workspaces.api.WorkspaceWorkflows
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import javax.inject.Inject

@HiltViewModel
class ProjectsViewModel @Inject constructor(private val workflows: WorkspaceWorkflows) :
    BaseMVIViewModel<ProjectsIntent, ProjectsState, ProjectsEffect>(ProjectsState()) {
    private var observation: Job? = null
    private var refreshJob: Job? = null
    private var generation = 0L
    private var needsRefresh = true
    private var savedProjects = emptyList<WorkflowProject>()

    override fun onIntent(intent: ProjectsIntent) {
        when (intent) {
            is ProjectsIntent.OpenProject -> if (savedProjects.any { it.id == intent.id }) sendEffect(ProjectsEffect.OpenProject(intent.id))
            ProjectsIntent.Discover -> sendEffect(ProjectsEffect.Discover)
            ProjectsIntent.Entered, ProjectsIntent.Reload -> {
                if (observation?.isActive != true) observe() else refresh()
            }
            is ProjectsIntent.StageSelected -> { reduce { copy(stage = intent.stage) }; publish() }
        }
    }
    private fun observe() {
        needsRefresh = true
        observation = safeLaunch {
            workflows.observeProjects().collect { snapshot ->
                if (state.value.userId != snapshot.userId) {
                    generation++
                    refreshJob?.cancel()
                    savedProjects = emptyList()
                    needsRefresh = true
                    reduce { ProjectsState(stage = stage, userId = snapshot.userId) }
                }
                if (snapshot.cacheReady && snapshot.cacheFailure == null) savedProjects = snapshot.value.orEmpty()
                reduce { copy(cacheReady = snapshot.cacheReady, failure = snapshot.cacheFailure ?: failure) }
                publish()
                if (snapshot.cacheReady && snapshot.userId != null && needsRefresh) {
                    needsRefresh = false
                    refresh()
                }
            }
        }
    }
    private fun stage(project: WorkflowProject) = when (project.analysisStatus) {
        WorkspaceStatus.NOT_STARTED -> AnalysisStage.WATCHING
        WorkspaceStatus.READY, WorkspaceStatus.STALE -> AnalysisStage.READY
        else -> AnalysisStage.IN_REVIEW
    }
    private fun publish() = reduce {
        val cards = savedProjects.filter { stage(it) == stage }
            .map { AnalysisProjectCard(it.id, it.name, "", "", it.analysisStatus) }
        copy(projects = cards, status = when {
            failure != null -> FeatureContentStatus.ERROR
            !cacheReady || (isRefreshing && cards.isEmpty()) -> FeatureContentStatus.LOADING
            cards.isEmpty() -> FeatureContentStatus.EMPTY
            else -> FeatureContentStatus.CONTENT
        })
    }
    private fun refresh() {
        val owner = state.value.userId ?: return
        if (!state.value.cacheReady || refreshJob?.isActive == true) return
        val ticket = generation
        reduce { copy(isRefreshing = true, failure = null) }; publish()
        refreshJob = safeLaunch {
            try { workflows.refreshProjects() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                if (ticket == generation && state.value.userId == owner) reduce {
                    copy(failure = (error as? WorkspaceException)?.failure ?: WorkspaceFailure.UNKNOWN)
                }
            } finally {
                if (ticket == generation && state.value.userId == owner) { reduce { copy(isRefreshing = false) }; publish() }
            }
        }
    }
    override fun onError(error: Throwable) {
        generation++; refreshJob?.cancel()
        reduce { copy(failure = (error as? WorkspaceException)?.failure ?: WorkspaceFailure.UNKNOWN,
            isRefreshing = false, status = FeatureContentStatus.ERROR) }
    }
}
