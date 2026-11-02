package com.example.problemfinder.feature.details.impl.details

import com.example.problemfinder.core.model.details.OpportunityDetails
import com.example.problemfinder.core.model.details.OpportunityEvidence
import com.example.problemfinder.core.model.opportunity.Opportunity
import com.example.problemfinder.core.model.opportunity.OpportunityFailure
import com.example.problemfinder.core.mvi.RequestState
import com.example.problemfinder.feature.details.impl.util.OpportunityDetailsUIError

/**
 * Fourth milestone: usable screen (DETAILS-5/6/7).
 * 5. OpportunityDetailsViewModel.kt: consume snapshots; coordinate refresh/page/watch jobs and errors.
 * 6. OpportunityDetailsScreen.kt + Navigation.kt: finish content/evidence/states and external links.
 * 7. implementation-guides/OpportunityDetailsViewModelTest.kt.template lists the behavior tests.
 *    Write tests during each milestone, not only after the full screen is finished.
 *
 * Runtime paths to keep separate:
 * Opening: route ID -> Entered -> observe use case -> repository -> Room -> snapshot -> state -> UI.
 * Refresh: Reload -> refresh use case -> HTTP DTO -> validated model -> entities -> Room transaction.
 * The Room update then travels through the FIRST path. Refresh does not return screen content.
 *
 * Enable/remove implementationPending only when the corresponding real operation is connected.
 * Passing builds alone do not complete a milestone: use each file's finish check before moving on.
 */
data class OpportunityDetailsState(
    val clusterId: Int? = null,
    val userId: String? = null,
    val details: OpportunityDetails? = null,
    val cachedOpportunity: Opportunity? = null,
    val topEvidence: List<OpportunityEvidence> = emptyList(),
    val posts: List<OpportunityEvidence> = emptyList(),
    val canLoadMore: Boolean = false,
    val cacheReady: Boolean = false,
    val hasSynced: Boolean = false,
    val observationRequest: RequestState<Unit, OpportunityDetailsUIError> = RequestState.Idle,
    val refreshRequest: RequestState<Unit, OpportunityDetailsUIError> = RequestState.Idle,
    val pageRequest: RequestState<Unit, OpportunityDetailsUIError> = RequestState.Idle,
    val isWatchPending: Boolean = false,
    val implementationPending: Boolean = true,
)

sealed interface OpportunityDetailsIntent {
    data class Entered(val clusterId: Int) : OpportunityDetailsIntent
    data object Reload : OpportunityDetailsIntent
    data object LoadNextEvidencePage : OpportunityDetailsIntent
    data object RetryEvidencePage : OpportunityDetailsIntent
    data object WatchClicked : OpportunityDetailsIntent
    data class OpenEvidence(val evidenceId: Int) : OpportunityDetailsIntent
    data object OpenEvidenceList : OpportunityDetailsIntent
    data object BackClicked : OpportunityDetailsIntent
}

sealed interface OpportunityDetailsEffect {
    data object NavigateBack : OpportunityDetailsEffect
    data class NavigateToEvidence(val evidence: Int) : OpportunityDetailsEffect
    data object NavigateToEvidenceList : OpportunityDetailsEffect
    data class ActionFailed(val failure: OpportunityDetailsUIError) : OpportunityDetailsEffect
}
