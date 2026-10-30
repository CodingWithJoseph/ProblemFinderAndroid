package com.example.problemfinder.feature.opportunitydetails.impl

import com.example.problemfinder.core.model.opportunity.OpportunityFailure
import com.example.problemfinder.core.model.opportunity.OpportunityOperationException
import com.example.problemfinder.core.mvi.BaseMVIViewModel
import com.example.problemfinder.core.mvi.RequestState
import com.example.problemfinder.domain.opportunities.api.actions.SetOpportunityWatchedUseCase
import com.example.problemfinder.domain.opportunities.api.details.LoadNextOpportunityEvidenceUseCase
import com.example.problemfinder.domain.opportunities.api.details.ObserveOpportunityDetailsUseCase
import com.example.problemfinder.domain.opportunities.api.details.RefreshOpportunityDetailsUseCase
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

    override fun onIntent(intent: OpportunityDetailsIntent) {
        when (intent) {
            is OpportunityDetailsIntent.Entered -> startObserving(intent.clusterId)
            is OpportunityDetailsIntent.OpenEvidence -> openEvidence(intent.evidenceId)
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

                if(version == refreshVersion){
                    reduce { copy(refreshRequest = RequestState.Success(Unit)) }
                }
            } catch (cancelled: CancellationException) {
                if(version == refreshVersion) {
                    reduce { copy(refreshRequest = RequestState.Idle) }
                }
                throw cancelled
            } catch (error: Exception ) {
                val failure = (error as? OpportunityOperationException)?.failure ?: OpportunityFailure.UNKNOWN

                if(version == refreshVersion) {
                    reduce { copy(refreshRequest = RequestState.Idle) }
                }
            }
        }
    }

    private fun loadMore() {
        // TODO [DETAILS-5C] Keep evidence append independent from page-1 refresh.
        // Require a synced selected ID/account, canLoadMore and no active page or replacement refresh job.
        // Capture ID/account/generation; set pageRequest Loading; call loadNextEvidence(id). Finish only that
        // matching request with Success/Error. Leave existing text/posts in state during append/failure.
        // The repository chooses the page number. Do not infer it from rendered rows/topEvidence counts.
        // RetryEvidencePage uses this same method and the unchanged saved progress after failure.
        // Finish check: tapping twice starts one append; page failure shows a footer retry, not a blank screen.
    }

    private fun watch() {
        // TODO [DETAILS-5D] Reuse the EXISTING membership flow, after details observation includes the card.
        // Require userId, cachedOpportunity and no active watch job. Capture current account/ID and the
        // desired value (!cachedOpportunity.isWatched), set isWatchPending, then call setWatched with those
        // explicit values. Keep the displayed watch value until Room emits the server-confirmed change.
        // Clear pending only for the same account/ID/generation; emit ActionFailed for an ordinary error.
        // Cancel/suppress old jobs on account/ID changes. Unknown membership on a cold link stays disabled.
        // Reference DiscoveryViewModel.watchCluster/cancelWatchActions for these race/error protections.
        // Finish check: Add here updates Discovery/Browse, and removing there appears here on return.
    }

    private fun openEvidence(evidenceId: Int) {
        // TODO [DETAILS-6] Resolve the clicked evidence ID against LOADED state, not an arbitrary caller URL.
        // Find the evidence in topEvidence/posts. If it has a usable source URL, emit OpenEvidenceLink(url).
        // For an absent/invalid URL, keep the row readable but its external-link action unavailable.
        // Navigation.kt owns opening the browser and reporting launch failure. The ViewModel owns neither
        // an Activity nor Android URL-launch APIs. Check: opening a post never changes the detail data state.
    }

    override fun onError(error: Throwable) {
        // TODO [DETAILS-5E] This fallback receives exceptions that escaped a safeLaunch block.
        // Handle refresh/page/watch errors inside those operations so they cannot masquerade as failed
        // cache observation. Reserve this path for observation/unexpected failures: stop dependent jobs,
        // map the stable failure category, end relevant Loading/pending states and keep useful content.
        // Retry must be able to restart the observer. Preserve cancellation instead of displaying it.
        // Finish check: losing the observer is visible/recoverable; one failed Add never blanks the details.
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
        // ill implement this when i implement watch logic
    }

    private fun cancelRefresh() {
        // ill implement this when i implement refresh logic
    }
}
