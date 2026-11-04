package com.example.problemfinder.feature.details.impl.details

import com.example.problemfinder.core.model.details.OpportunityDetails
import com.example.problemfinder.core.model.details.OpportunityEvidence
import com.example.problemfinder.core.model.opportunity.Opportunity
import com.example.problemfinder.core.mvi.RequestState
import com.example.problemfinder.feature.details.impl.util.OpportunityDetailsUIError

/** Cached Details content and the independent lifecycles of observation, refresh, and paging. */
data class OpportunityDetailsState(
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
) {
    val canRefresh: Boolean get() = refreshRequest !is RequestState.Loading && pageRequest !is RequestState.Loading &&
        (observationRequest is RequestState.Error || (cacheReady && userId != null && observationRequest is RequestState.Success))

    val canPage: Boolean get() = cacheReady && hasSynced && canLoadMore && userId != null &&
        observationRequest is RequestState.Success && refreshRequest !is RequestState.Loading && pageRequest !is RequestState.Loading

    val canWatch: Boolean get() = cachedOpportunity != null && !isWatchPending &&
        cacheReady && userId != null && observationRequest is RequestState.Success
}

sealed interface OpportunityDetailsIntent {
    data object Reload : OpportunityDetailsIntent
    data object LoadNextEvidencePage : OpportunityDetailsIntent
    data object RetryEvidencePage : OpportunityDetailsIntent
    data object WatchClicked : OpportunityDetailsIntent
    data class OpenEvidence(val evidenceId: String) : OpportunityDetailsIntent
    data object OpenEvidenceList : OpportunityDetailsIntent
    data object BackClicked : OpportunityDetailsIntent
}

sealed interface OpportunityDetailsEffect {
    data object NavigateBack : OpportunityDetailsEffect
    data class NavigateToEvidence(val evidence: String) : OpportunityDetailsEffect
    data object NavigateToEvidenceList : OpportunityDetailsEffect
    data class ActionFailed(val failure: OpportunityDetailsUIError) : OpportunityDetailsEffect
}
