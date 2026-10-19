package com.example.problemfinder.feature.opportunities.impl.discovery

import com.example.problemfinder.core.model.opportunity.OpportunityFailure
import com.example.problemfinder.core.model.opportunity.OpportunityOperationException
import com.example.problemfinder.domain.opportunities.api.actions.SetOpportunityWatchedUseCase
import com.example.problemfinder.core.mvi.BaseMVIViewModel
import com.example.problemfinder.core.mvi.RequestState
import com.example.problemfinder.domain.opportunities.api.discovery.ObserveDiscoveryUseCase
import com.example.problemfinder.domain.opportunities.api.discovery.RefreshDiscoveryUseCase
import com.example.problemfinder.feature.opportunities.impl.util.OpportunityUiError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import javax.inject.Inject

@HiltViewModel
class DiscoveryViewModel @Inject constructor(
    private val observeDiscoveryUseCase: ObserveDiscoveryUseCase,
    private val refreshDiscoveryUseCase: RefreshDiscoveryUseCase,
    private val setWatched: SetOpportunityWatchedUseCase,
) : BaseMVIViewModel<DiscoveryIntent, DiscoveryState, DiscoveryEffect>(
    DiscoveryState()
) {
    private var needsInitialRefresh = true
    private val watchJobs = mutableMapOf<Int, Job>()
    private var watchVersion = 0L
    private var observationJob: Job? = null
    private var refreshJob: Job? = null
    private var refreshVersion: Long = 0L

    override fun onIntent(intent: DiscoveryIntent) {
        when (intent) {
            DiscoveryIntent.Entered -> startObserving()
            DiscoveryIntent.Reload -> reloadDashboard()
            is DiscoveryIntent.WatchCluster -> watchCluster(intent.clusterId)

            is DiscoveryIntent.OpenCluster -> {
                sendEffect(
                    DiscoveryEffect.NavigateToCluster(intent.clusterId)
                )
            }
            is DiscoveryIntent.SeeAll -> {
                sendEffect(
                    DiscoveryEffect.NavigateToBrowse(intent.clusterCategory)
                )
            }
        }
    }

    private fun startObserving() {
        if (observationJob?.isActive == true) return

        needsInitialRefresh = true

        reduce {
            copy(observationRequest = RequestState.Loading)
        }

        observationJob = safeLaunch {
            observeDiscoveryUseCase(Unit).collect { snapshot ->

                if (state.value.userId != snapshot.userId) {
                    cancelWatchActions()
                    cancelRefresh()
                    needsInitialRefresh = true
                }

                reduce {
                    val current = if (userId == snapshot.userId) {
                        this
                    } else {
                        DiscoveryState(userId = snapshot.userId)
                    }

                    if (!snapshot.cacheReady) {
                        current.copy(
                            cacheReady = false,
                            observationRequest = RequestState.Loading
                        )
                    } else {
                        current.copy(
                            featuredCluster = snapshot.dashboard.featuredCluster,
                            collections = snapshot.dashboard.collections,
                            hasSynced = snapshot.hasSynced,
                            cacheReady = true,
                            observationRequest = RequestState.Success(Unit)
                        )
                    }
                }

                if (snapshot.cacheReady && snapshot.userId != null && needsInitialRefresh){
                    needsInitialRefresh = false
                    reloadDashboard()
                }
            }
        }
    }

    private fun reloadDashboard() {
        val current = state.value

        if(current.observationRequest is RequestState.Error){
            startObserving()
            return
        }

        if (
            current.userId == null ||
            !current.cacheReady ||
            current.observationRequest !is RequestState.Success ||
            current.refreshRequestState == RequestState.Loading
        ) return

        refreshVersion += 1L
        val version = refreshVersion

        reduce {
            copy(refreshRequestState = RequestState.Loading)
        }

        refreshJob = safeLaunch {
            try {
                refreshDiscoveryUseCase(Unit)

                if (version == refreshVersion) {
                    reduce {
                        copy(refreshRequestState = RequestState.Success(Unit))
                    }
                }
            } catch (cancelled: CancellationException) {
                if (version == refreshVersion) {
                    reduce {
                        copy(refreshRequestState = RequestState.Idle)
                    }
                }
                throw cancelled
            } catch (error: Exception) {
                val failure = (error as? OpportunityOperationException)?.failure ?: OpportunityFailure.UNKNOWN
                if (version == refreshVersion) {
                    reduce {
                        copy(refreshRequestState = RequestState.Error(OpportunityUiError(failure)))
                    }
                }
            }
        }
    }

    private fun watchCluster(clusterId: Int) {
        val current = state.value
        val userId = current.userId ?: return
        if (!current.cacheReady || clusterId in current.pendingClusterIds) return
        val cluster = current.featuredCluster?.takeIf { it.id == clusterId }
            ?: current.collections.asSequence().flatMap { it.clusters }.firstOrNull { it.id == clusterId } ?: return
        val version = watchVersion
        reduce { copy(pendingClusterIds = pendingClusterIds + clusterId) }
        val job = safeLaunch {
            try {
                setWatched(SetOpportunityWatchedUseCase.Params(userId, clusterId, !cluster.isWatched))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (version == watchVersion && state.value.userId == userId) {
                    val failure = (error as? OpportunityOperationException)?.failure ?: OpportunityFailure.UNKNOWN
                    sendEffect(DiscoveryEffect.ShowError(OpportunityUiError(failure)))
                }
            } finally {
                if (version == watchVersion && state.value.userId == userId) {
                    reduce { copy(pendingClusterIds = pendingClusterIds - clusterId) }
                }
            }
        }
        watchJobs[clusterId] = job
        job.invokeOnCompletion { if (watchJobs[clusterId] === job) watchJobs.remove(clusterId) }
    }

    private fun cancelWatchActions() {
        watchVersion += 1
        watchJobs.values.toList().forEach { it.cancel() }
        watchJobs.clear()
    }

    private fun cancelRefresh() {
        refreshVersion += 1
        refreshJob?.cancel()
        refreshJob = null
    }

    override fun onError(error: Throwable) {
        cancelRefresh()

        val failure = (error as? OpportunityOperationException)?.failure
            ?: OpportunityFailure.UNKNOWN

        reduce {
            copy(
                observationRequest = RequestState.Error(
                    OpportunityUiError(failure)
                ),
                refreshRequestState = RequestState.Idle,
            )
        }
    }
}
