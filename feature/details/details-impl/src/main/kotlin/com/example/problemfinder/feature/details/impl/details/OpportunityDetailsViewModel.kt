package com.example.problemfinder.feature.details.impl.details

import com.example.problemfinder.core.model.opportunity.OpportunityFailure
import com.example.problemfinder.core.model.opportunity.OpportunityOperationException
import com.example.problemfinder.core.mvi.BaseMVIViewModel
import com.example.problemfinder.core.mvi.RequestState
import com.example.problemfinder.domain.opportunities.api.actions.SetOpportunityWatchedUseCase
import com.example.problemfinder.domain.opportunities.api.details.LoadNextOpportunityEvidenceUseCase
import com.example.problemfinder.domain.opportunities.api.details.ObserveOpportunityDetailsUseCase
import com.example.problemfinder.domain.opportunities.api.details.RefreshOpportunityDetailsUseCase
import com.example.problemfinder.feature.details.impl.util.OpportunityDetailsUIError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import javax.inject.Inject

@HiltViewModel
class OpportunityDetailsViewModel @Inject constructor(
    private val observeDetails: ObserveOpportunityDetailsUseCase,
    private val refreshDetails: RefreshOpportunityDetailsUseCase,
    private val loadNextEvidence: LoadNextOpportunityEvidenceUseCase,
    private val setWatched: SetOpportunityWatchedUseCase,
) : BaseMVIViewModel<OpportunityDetailsIntent, OpportunityDetailsState, OpportunityDetailsEffect>(
    OpportunityDetailsState()
) {

    private var needsInitialRefresh = true

    private var observationJob: Job? = null
    private var activeId: Int = -1

    private var refreshJob: Job? = null
    private var refreshVersion = 0L

    private var loadNextJob: Job? = null
    private var loadVersion = 0L

    private var watchJobs: MutableMap<Int, Job> = mutableMapOf()
    private var watchVersion = 0L

    override fun onIntent(intent: OpportunityDetailsIntent) {
        when (intent) {
            is OpportunityDetailsIntent.Entered -> startObserving(intent.clusterId)
            is OpportunityDetailsIntent.OpenEvidence -> openEvidence(intent.evidenceId)
            OpportunityDetailsIntent.OpenEvidenceList -> sendEffect(OpportunityDetailsEffect.NavigateToEvidenceList)
            OpportunityDetailsIntent.Reload -> reload()
            OpportunityDetailsIntent.LoadNextEvidencePage,
            OpportunityDetailsIntent.RetryEvidencePage -> loadMore()

            OpportunityDetailsIntent.WatchClicked -> watch()
            OpportunityDetailsIntent.BackClicked -> sendEffect(OpportunityDetailsEffect.NavigateBack)
        }
    }

    private fun startObserving(clusterId: Int) {
        if (clusterId <= 0) return onError(
            OpportunityOperationException(
                OpportunityFailure.CLUSTER_NOT_FOUND,
                Exception()
            )
        )

        if (observationJob?.isActive == true && clusterId == state.value.clusterId) return
        cancelJobs()

        observationJob = safeLaunch {
            reduce {
                copy(clusterId = clusterId)
            }

            observeDetails(ObserveOpportunityDetailsUseCase.Params(clusterId)).collect { response ->
                if (state.value.userId != response.userId) {
                    cancelWatch()
                    cancelRefresh()
                    needsInitialRefresh = true
                }

                reduce {
                    val current = if (userId == response.userId) {
                        this
                    } else {
                        OpportunityDetailsState(userId = response.userId)
                    }

                    if (response.cacheReady.not()) {
                        current.copy(
                            cacheReady = false,
                            clusterId = clusterId,
                            observationRequest = RequestState.Loading
                        )
                    } else {
                        current.copy(
                            details = response.details,
                            clusterId = clusterId,
                            cachedOpportunity = response.cachedOpportunity,
                            topEvidence = response.topEvidence,
                            posts = response.posts,
                            cacheReady = response.cacheReady,
                            hasSynced = response.hasSynced,
                            canLoadMore = (response.pageSize * response.lastLoadedPage) < response.totalPosts,
                            observationRequest = RequestState.Success(Unit)
                        )
                    }
                }

                if (response.cacheReady && response.userId != null && needsInitialRefresh) {
                    needsInitialRefresh = false
                    reload()
                }

            }
        }


    }

    private fun reload() {
        val current = state.value

        if ((current.clusterId == null) || (current.clusterId <= 0))
            return onError(
                OpportunityOperationException(
                    OpportunityFailure.CLUSTER_NOT_FOUND,
                    Exception()
                )
            )

        if (
            current.userId == null ||
            current.cacheReady.not() ||
            current.refreshRequest is RequestState.Loading
        ) return

        refreshVersion += 1L
        val version = refreshVersion

        reduce { copy(refreshRequest = RequestState.Loading) }

        refreshJob = safeLaunch {
            try {
                refreshDetails(
                    RefreshOpportunityDetailsUseCase.Params(
                        clusterId = current.clusterId
                    )
                )

                if (version == refreshVersion) {
                    reduce { copy(refreshRequest = RequestState.Success(Unit)) }
                }
            } catch (cancelled: CancellationException) {
                if (version == refreshVersion) {
                    reduce { copy(refreshRequest = RequestState.Idle) }
                }
                throw cancelled
            } catch (error: Exception) {
                val failure =
                    (error as? OpportunityOperationException)?.failure ?: OpportunityFailure.UNKNOWN

                if (version == refreshVersion) {
                    reduce { copy(refreshRequest = RequestState.Error(OpportunityDetailsUIError(failure))) }
                }
            }
        }
    }

    private fun loadMore() {
        val current = state.value
        val clusterId = current.clusterId

        if (clusterId == null || clusterId <= 0) return onError(
            OpportunityOperationException(
                OpportunityFailure.CLUSTER_NOT_FOUND,
                Exception()
            )
        )

        if (
            current.userId.isNullOrEmpty() ||
            current.canLoadMore.not() ||
            current.cacheReady.not() ||
            refreshJob?.isActive == true
        ) return

        loadVersion += 1L
        val version = loadVersion

        try {
            loadNextJob = safeLaunch {
                loadNextEvidence(LoadNextOpportunityEvidenceUseCase.Params(clusterId))
            }

            if (loadVersion == version) {
                reduce {
                    copy(
                        pageRequest = RequestState.Success(Unit)
                    )
                }
            }
        } catch (cancelled: CancellationException) {
            if (loadVersion == version) {
                reduce {
                    copy(
                        pageRequest = RequestState.Idle
                    )
                }
            }
            throw cancelled
        } catch (exception: Exception) {
            val failure = (exception as? OpportunityOperationException)?.failure ?: OpportunityFailure.UNKNOWN
            if (loadVersion == version) {
                reduce {
                    copy(
                        pageRequest = RequestState.Error(OpportunityDetailsUIError(failure)),
                    )
                }
            }
        }
    }

    private fun watch() {
        val current = state.value
        val opportunity = current.cachedOpportunity
        if (
            current.userId == null ||
            current.clusterId == null ||
            opportunity == null ||
            opportunity.isWatched.not() ||
            current.cacheReady.not() ||
            refreshJob?.isActive == true ||
            loadNextJob?.isActive == true
        ) return

        reduce { copy(isWatchPending = true) }

        watchVersion = 1L
        val version = watchVersion

        val job = safeLaunch {
            try {
                setWatched(
                    SetOpportunityWatchedUseCase.Params(
                        userId = current.userId,
                        clusterId = current.clusterId,
                        watched = opportunity.isWatched
                    )
                )

                if (watchVersion == version) {

                }
            } catch (cancelled: CancellationException) {
                if (loadVersion == version) {
                    reduce {
                        copy(
                            pageRequest = RequestState.Idle
                        )
                    }
                }
                throw cancelled
            } catch (exception: Exception) {
                val failure = (exception as? OpportunityOperationException)?.failure
                    ?: OpportunityFailure.UNKNOWN
                if (loadVersion == version) {
                    reduce {
                        copy(
                            pageRequest = RequestState.Error(OpportunityDetailsUIError(failure)),
                        )
                    }
                }
            }
        }
        watchJobs[opportunity.id] = job
        job.invokeOnCompletion {
            if(watchJobs[current.clusterId] == job)
                watchJobs.remove(current.clusterId)
        }
    }

    private fun openEvidence(evidenceId: Int) = sendEffect(
        OpportunityDetailsEffect.NavigateToEvidence(evidenceId)
    )

    override fun onError(error: Throwable) {
        cancelRefresh()

        val failure = (error as? OpportunityOperationException)?.failure ?: OpportunityFailure.UNKNOWN

        reduce {
            copy(
                observationRequest = RequestState.Error(OpportunityDetailsUIError(failure)),
                refreshRequest = RequestState.Idle,
            )
        }
    }

    private fun cancelJobs() {
        cancelObserve()
        cancelWatch()
        cancelRefresh()
    }

    private fun cancelObserve() {
        activeId = -1
        observationJob?.cancel()
        observationJob = null
    }

    private fun cancelWatch() {
        watchVersion += 1L
        watchJobs.values.toList().forEach { it.cancel() }
        watchJobs.clear()
    }

    private fun cancelRefresh() {
        refreshVersion += 1L
        refreshJob?.cancel()
        refreshJob = null
    }
}
