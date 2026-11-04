package com.example.problemfinder.feature.details.impl.details

import androidx.lifecycle.SavedStateHandle
import com.example.problemfinder.core.model.opportunity.OpportunityFailure
import com.example.problemfinder.core.model.opportunity.OpportunityOperationException
import com.example.problemfinder.core.mvi.BaseMVIViewModel
import com.example.problemfinder.core.mvi.RequestState
import com.example.problemfinder.feature.details.api.OpportunityDetailsDestination as Routes
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
    savedStateHandle: SavedStateHandle,
    private val observeDetails: ObserveOpportunityDetailsUseCase,
    private val refreshDetails: RefreshOpportunityDetailsUseCase,
    private val loadNextEvidence: LoadNextOpportunityEvidenceUseCase,
    private val setWatched: SetOpportunityWatchedUseCase,
) : BaseMVIViewModel<OpportunityDetailsIntent, OpportunityDetailsState, OpportunityDetailsEffect>(
    OpportunityDetailsState()
) {
    private val clusterId: Int = requireNotNull(
        savedStateHandle[Routes.ID_ARGUMENT]
    )
    private var needsInitialRefresh = true

    private var observationJob: Job? = null

    private var refreshJob: Job? = null
    private var refreshVersion = 0L

    private var loadNextJob: Job? = null
    private var loadVersion = 0L

    private var watchJob: Job? = null
    private var watchVersion = 0L

    init {
        require(clusterId > 0)
        startObserving()
    }

    override fun onIntent(intent: OpportunityDetailsIntent) {
        when (intent) {
            is OpportunityDetailsIntent.OpenEvidence -> openEvidence(intent.evidenceId)
            OpportunityDetailsIntent.Reload -> reload()
            OpportunityDetailsIntent.WatchClicked -> watch()
            OpportunityDetailsIntent.LoadNextEvidencePage,
            OpportunityDetailsIntent.RetryEvidencePage -> loadMore()
            OpportunityDetailsIntent.BackClicked -> sendEffect(OpportunityDetailsEffect.NavigateBack)
            OpportunityDetailsIntent.OpenEvidenceList -> sendEffect(OpportunityDetailsEffect.NavigateToEvidenceList)
        }
    }

    private fun startObserving() {
        if (observationJob?.isActive == true) return
        needsInitialRefresh = true
        reduce { copy(cacheReady = false, observationRequest = RequestState.Loading) }

        observationJob = safeLaunch {
            observeDetails(ObserveOpportunityDetailsUseCase.Params(clusterId)).collect { response ->
                if (state.value.userId != response.userId) {
                    needsInitialRefresh = true
                    cancelWatch()
                    cancelRefresh()
                    cancelLoadMore()
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
                            observationRequest = RequestState.Loading
                        )
                    } else {
                        current.copy(
                            details = response.details,
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
                    refresh()
                }

            }
        }


    }

    private fun reload() {
        if (observationJob?.isActive != true) {
            startObserving()
            return
        }
        refresh()
    }

    private fun refresh() {
        val current = state.value
        if (current.userId.isNullOrEmpty() || !current.cacheReady ||
            current.observationRequest !is RequestState.Success ||
            current.refreshRequest is RequestState.Loading || current.pageRequest is RequestState.Loading
        ) return
        needsInitialRefresh = false

        refreshVersion += 1L
        val version = refreshVersion

        reduce { copy(refreshRequest = RequestState.Loading) }

        refreshJob = safeLaunch {
            try {
                refreshDetails(
                    RefreshOpportunityDetailsUseCase.Params(clusterId = clusterId)
                )

                if (version == refreshVersion) {
                    reduce { copy(refreshRequest = RequestState.Success(Unit), pageRequest = RequestState.Idle) }
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
                    reduce {
                        copy(
                            refreshRequest = RequestState.Error(
                                OpportunityDetailsUIError(
                                    failure
                                )
                            )
                        )
                    }
                }
            }
        }
    }

    private fun loadMore() {
        val current = state.value
        if (
            current.observationRequest !is RequestState.Success ||
            current.hasSynced.not() ||
            current.cacheReady.not() ||
            current.canLoadMore.not() ||
            current.refreshRequest is RequestState.Loading ||
            current.pageRequest is RequestState.Loading ||
            current.userId.isNullOrEmpty()
        ) return

        loadVersion += 1L
        val version = loadVersion

        reduce { copy(pageRequest = RequestState.Loading) }

        loadNextJob = safeLaunch {
            try {
                loadNextEvidence(LoadNextOpportunityEvidenceUseCase.Params(clusterId))

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
                val failure =
                    (exception as? OpportunityOperationException)?.failure ?: OpportunityFailure.UNKNOWN
                if (loadVersion == version) {
                    reduce {
                        copy(
                            pageRequest = RequestState.Error(OpportunityDetailsUIError(failure)),
                        )
                    }
                }
            }
        }
    }

    private fun watch() {
        val current = state.value
        val user = current.userId ?: return
        val opportunity = current.cachedOpportunity ?: return
        if (!current.canWatch) return

        val version = ++watchVersion
        reduce { copy(isWatchPending = true) }
        watchJob = safeLaunch {
            try {
                setWatched(SetOpportunityWatchedUseCase.Params(user, clusterId, !opportunity.isWatched))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (version == watchVersion) {
                    val failure = (error as? OpportunityOperationException)?.failure ?: OpportunityFailure.UNKNOWN
                    sendEffect(OpportunityDetailsEffect.ActionFailed(OpportunityDetailsUIError(failure)))
                }
            } finally {
                if (version == watchVersion) reduce { copy(isWatchPending = false) }
            }
        }
    }

    private fun openEvidence(evidenceId: String) = sendEffect(
        OpportunityDetailsEffect.NavigateToEvidence(evidenceId)
    )

    override fun onError(error: Throwable) {
        cancelRefresh()
        cancelLoadMore()
        cancelWatch()

        val failure =
            (error as? OpportunityOperationException)?.failure ?: OpportunityFailure.UNKNOWN

        reduce {
            copy(
                cacheReady = false,
                observationRequest = RequestState.Error(OpportunityDetailsUIError(failure)),
                refreshRequest = RequestState.Idle,
                pageRequest = RequestState.Idle,
                isWatchPending = false,
            )
        }
    }

    private fun cancelWatch() {
        watchVersion += 1L
        watchJob?.cancel()
        watchJob = null
    }

    private fun cancelRefresh() {
        refreshVersion += 1L
        refreshJob?.cancel()
        refreshJob = null
    }

    private fun cancelLoadMore() {
        loadVersion += 1L
        loadNextJob?.cancel()
        loadNextJob = null
    }
}
