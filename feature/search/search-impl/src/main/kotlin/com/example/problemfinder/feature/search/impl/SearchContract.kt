package com.example.problemfinder.feature.search.impl

import com.example.problemfinder.core.model.opportunity.Opportunity
import com.example.problemfinder.core.model.opportunity.OpportunityFailure
import com.example.problemfinder.core.model.search.OpportunitySearchDraft
import com.example.problemfinder.core.model.search.OpportunitySearchQuery
import com.example.problemfinder.core.mvi.RequestState

/** Editable input and the independently submitted result set. */
data class SearchState(
    val draftQuery: OpportunitySearchQuery = OpportunitySearchQuery(),
    val submittedQuery: OpportunitySearchQuery? = null,
    val interpretation: OpportunitySearchDraft? = null,
    val userId: String? = null,
    val items: List<Opportunity> = emptyList(),
    val total: Int = 0,
    val canLoadMore: Boolean = false,
    val cacheReady: Boolean = false,
    val hasSynced: Boolean = false,
    val observationRequest: RequestState<Unit, OpportunityFailure> = RequestState.Idle,
    val refreshRequest: RequestState<Unit, OpportunityFailure> = RequestState.Idle,
    val pageRequest: RequestState<Unit, OpportunityFailure> = RequestState.Idle,
    val interpretationRequest: RequestState<Unit, OpportunityFailure> = RequestState.Idle,
    val pendingClusterIds: Set<Int> = emptySet(),
    val filterDraft: OpportunitySearchQuery? = null,
    val assistantInput: String = "",
    val optionsRequest: RequestState<Unit, OpportunityFailure> = RequestState.Idle,
    val sourceOptions: List<String> = emptyList(),
    val communityOptions: List<String> = emptyList(),
    val minPostsInput: String = "1",
    val observedAfterInput: String = "",
    val validationMessage: Int? = null,
) {
    val canRefresh: Boolean get() = submittedQuery != null &&
        refreshRequest !is RequestState.Loading && pageRequest !is RequestState.Loading &&
        (observationRequest is RequestState.Error || (cacheReady && userId != null && observationRequest is RequestState.Success))

    val canAppend: Boolean get() = canLoadMore && cacheReady && observationRequest is RequestState.Success &&
        refreshRequest !is RequestState.Loading && pageRequest !is RequestState.Loading
}

sealed interface SearchIntent {
    data object Entered : SearchIntent
    data object OpenFilters : SearchIntent
    data object RetryOptions : SearchIntent
    data class AssistantChanged(val value: String) : SearchIntent
    data object OpenAssistant : SearchIntent
    data object ClosePanel : SearchIntent
    data class SourceToggled(val value: String) : SearchIntent
    data class CommunityToggled(val value: String) : SearchIntent
    data class MinimumPostsChanged(val value: String) : SearchIntent
    data class ObservedAfterChanged(val value: String) : SearchIntent
    data class SortSelected(val value: com.example.problemfinder.core.model.search.OpportunitySearchSort) : SearchIntent
    data object ApplyFilters : SearchIntent
    data object ResetFilters : SearchIntent
    data class QueryChanged(val value: String) : SearchIntent
    data class FiltersChanged(val value: OpportunitySearchQuery) : SearchIntent
    data object Submit : SearchIntent
    data object Reload : SearchIntent
    data object LoadNextPage : SearchIntent
    data object RetryNextPage : SearchIntent
    data object InterpretQuery : SearchIntent
    data object ConfirmDraft : SearchIntent
    data class OpenCluster(val clusterId: Int) : SearchIntent
    data class WatchCluster(val clusterId: Int) : SearchIntent
}

sealed interface SearchEffect {
    data object NavigateToFilters : SearchEffect
    data object NavigateToAssistant : SearchEffect
    data object NavigateBack : SearchEffect
    data class NavigateToCluster(val clusterId: Int) : SearchEffect
    data class ActionFailed(val failure: OpportunityFailure) : SearchEffect
}
