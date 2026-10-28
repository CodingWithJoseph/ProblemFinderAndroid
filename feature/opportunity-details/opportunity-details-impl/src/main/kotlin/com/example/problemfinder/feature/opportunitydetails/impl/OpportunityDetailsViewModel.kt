package com.example.problemfinder.feature.opportunitydetails.impl

import com.example.problemfinder.core.model.opportunity.OpportunityFailure
import com.example.problemfinder.core.model.opportunity.OpportunityOperationException
import com.example.problemfinder.core.mvi.BaseMVIViewModel
import com.example.problemfinder.domain.opportunities.api.actions.SetOpportunityWatchedUseCase
import com.example.problemfinder.domain.opportunities.api.details.LoadNextOpportunityEvidenceUseCase
import com.example.problemfinder.domain.opportunities.api.details.ObserveOpportunityDetailsUseCase
import com.example.problemfinder.domain.opportunities.api.details.RefreshOpportunityDetailsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
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

    // job, id
    private var observationJob: Job? = null
    private var activeId: Int = -1

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
        if (observationJob?.isActive == true ) return
        if (clusterId == activeId) return

        if (clusterId <= 0) onError(
            OpportunityOperationException(
                OpportunityFailure.CLUSTER_NOT_FOUND,
                Exception()
            )
        )

        cancelObserve()

        observationJob = safeLaunch {
            observeDetails(ObserveOpportunityDetailsUseCase.Params(clusterId)).collect { response ->
                reduce {
                    copy(
                        details = response.details,
                        cachedOpportunity = response.cachedOpportunity,
                        topEvidence = response.topEvidence,
                        posts = response.posts,
                        canLoadMore = (response.pageSize * response.lastLoadedPage) < response.totalPosts,
                        cacheReady = response.cacheReady,
                        hasSynced = response.hasSynced
                    )
                }
            }
        }

        reduce { copy(clusterId = clusterId) }
        // TODO [DETAILS-5A] Start this after the repository can emit real account-scoped snapshots.
        // 1. Reject a non-positive ID. Retain the selected ID and one observation Job; same-ID re-entry
        //    should reuse the active observation instead of starting another collector/initial refresh.

        // 2. For a different ID, invalidate request generations, cancel old refresh/page/watch work and
        //    clear old content BEFORE collecting the new use case. Account changes need the same cleanup.

        // 3. Collect observeDetails(clusterId) in safeLaunch. Reduce details, cachedOpportunity, evidence,
        //    userId, paging availability, cacheReady and hasSynced from the snapshot into screen state.

        // 4. Let the first completed cache read display before triggering ONE initial refresh for a known
        //    account. Later Room emissions from page/watch updates must not trigger infinite refreshes.

        // Reference DiscoveryViewModel.startObserving for jobs, account switching and the one-refresh flag;
        // add cluster identity to its guards. No DTO, HttpClient or DAO belongs in this ViewModel.
        // Finish check: repeated Entered creates one subscription; cached details appear while HTTP waits.
    }

    private fun reload() {
        // TODO [DETAILS-5B] Change request state here; content remains owned by the snapshot collector.
        // 1. Require a selected valid ID, known account and a ready observer. If observation failed,
        //    restart it first. Ignore a second refresh tap while this operation is already running.
        // 2. Capture ID/account plus a new request generation and set refreshRequest=Loading.
        // 3. In a retained Job call refreshDetails(id). On success set this request to Success(Unit) only
        //    if generation/ID/account still match. The observer, not this return value, updates details.
        // 4. Catch this operation's ordinary failures here and map to refreshRequest.Error while retaining
        //    content. Preserve cancellation. A late catch/finally must not touch a newer screen's state.
        // 5. Give HTTP 404 a detail-specific missing-content outcome; shared REQUEST_REJECTED also covers
        //    other client errors and cannot alone tell the screen that the cluster was deleted.
        // Finish check: refresh failure preserves readable text and permits retry; no spinner stays stuck.
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

    private fun cancelObserve() {
        activeId = -1
        observationJob?.cancel()
        observationJob = null
    }
}
