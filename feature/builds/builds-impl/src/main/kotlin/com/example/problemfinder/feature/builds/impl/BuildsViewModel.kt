package com.example.problemfinder.feature.builds.impl

import com.example.problemfinder.core.designsystem.components.FeatureContentStatus
import com.example.problemfinder.core.model.workspace.*
import com.example.problemfinder.core.mvi.BaseMVIViewModel
import com.example.problemfinder.domain.workspaces.api.WorkspaceWorkflows
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import javax.inject.Inject

@HiltViewModel
class BuildsViewModel @Inject constructor(private val workflows: WorkspaceWorkflows) :
    BaseMVIViewModel<BuildsIntent, BuildsState, BuildsEffect>(BuildsState()) {
    private var observation: Job? = null
    private var refreshJob: Job? = null
    private var generation = 0L
    private var needsRefresh = true

    override fun onIntent(intent: BuildsIntent) {
        when (intent) {
            is BuildsIntent.OpenProject -> if (state.value.projects.any { it.id == intent.id }) sendEffect(BuildsEffect.OpenProject(intent.id))
            BuildsIntent.OpenAnalyze -> sendEffect(BuildsEffect.OpenAnalyze)
            BuildsIntent.Entered, BuildsIntent.Reload ->
                if (observation?.isActive != true) observe() else refresh()
        }
    }
    private fun observe() {
        needsRefresh = true
        observation = safeLaunch {
            workflows.observeProjects().collect { snapshot ->
                if (state.value.userId != snapshot.userId) {
                    generation++; refreshJob?.cancel(); needsRefresh = true
                    reduce { BuildsState(userId = snapshot.userId) }
                }
                val cards = if (!snapshot.cacheReady || snapshot.cacheFailure != null) state.value.projects else snapshot.value.orEmpty().filter {
                    (it.analysisStatus in setOf(WorkspaceStatus.READY, WorkspaceStatus.STALE) ||
                        it.buildStatus != WorkspaceStatus.NOT_STARTED || it.stage == "building")
                }.map { BuildProjectCard(it.id, it.name, "", "", it.buildStatus) }
                reduce { copy(projects = cards, cacheReady = snapshot.cacheReady, failure = snapshot.cacheFailure ?: failure) }; publish()
                if (snapshot.cacheReady && snapshot.userId != null && needsRefresh) {
                    needsRefresh = false; refresh()
                }
            }
        }
    }
    private fun publish() = reduce { copy(status = when {
        failure != null -> FeatureContentStatus.ERROR
        !cacheReady || (isRefreshing && projects.isEmpty()) -> FeatureContentStatus.LOADING
        projects.isEmpty() -> FeatureContentStatus.EMPTY
        else -> FeatureContentStatus.CONTENT
    }) }
    private fun refresh() {
        val owner = state.value.userId ?: return
        if (!state.value.cacheReady || refreshJob?.isActive == true) return
        val ticket = generation
        reduce { copy(isRefreshing = true, failure = null) }; publish()
        refreshJob = safeLaunch {
            try { workflows.refreshProjects() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { if (ticket == generation && state.value.userId == owner) reduce {
                copy(failure = (error as? WorkspaceException)?.failure ?: WorkspaceFailure.UNKNOWN)
            } } finally {
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
