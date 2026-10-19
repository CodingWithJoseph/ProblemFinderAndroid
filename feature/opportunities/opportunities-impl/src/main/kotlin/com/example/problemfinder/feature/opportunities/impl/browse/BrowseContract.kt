package com.example.problemfinder.feature.opportunities.impl.browse

import com.example.problemfinder.core.common.shared.OpportunityCategoryFilter
import com.example.problemfinder.core.common.shared.SortType
import com.example.problemfinder.core.model.opportunity.Opportunity
import com.example.problemfinder.core.mvi.RequestState
import com.example.problemfinder.feature.opportunities.impl.util.OpportunityUiError

/**
 * Immutable rendering state for one account and category/sort query.
 * [cacheReady] means the local read completed; [hasSynced] records a successful server sync,
 * so an unsynced empty cache is distinguishable from a confirmed empty result.
 * Observation, refresh, and paging have separate request states to preserve usable cached rows.
 */
data class BrowseState(
    val category: OpportunityCategoryFilter = OpportunityCategoryFilter.ALL,
    val sort: SortType = SortType.NEWEST,
    val userId: String? = null,
    val data: List<Opportunity> = emptyList(),
    val pendingClusterIds: Set<Int> = emptySet(),
    val cursor: String? = null,
    val hasSynced: Boolean = false,
    val cacheReady: Boolean = false,
    val observationRequest: RequestState<Unit, OpportunityUiError> = RequestState.Loading,
    val refreshRequest: RequestState<Unit, OpportunityUiError> = RequestState.Idle,
    val pageRequest: RequestState<Unit, OpportunityUiError> = RequestState.Idle,
) {
    val isBusy: Boolean
        get() = refreshRequest == RequestState.Loading ||
            pageRequest == RequestState.Loading

    /** Page failures belong in the list footer, not the whole-content error presentation. */
    val contentError: OpportunityUiError?
        get() {
            val observation = observationRequest
            if (observation is RequestState.Error) return observation.error
            val refresh = refreshRequest
            return if (refresh is RequestState.Error) refresh.error else null
        }

    val canRefresh: Boolean
        get() = !isBusy && (
            observationRequest is RequestState.Error ||
                (cacheReady && userId != null)
            )

    val canLoadNextPage: Boolean
        get() = cacheReady && userId != null && hasSynced && cursor != null &&
            !isBusy && observationRequest !is RequestState.Error

    /** A failed page requires a deliberate retry rather than repeated scroll-triggered requests. */
    val canAutoLoadNextPage: Boolean
        get() = canLoadNextPage && pageRequest !is RequestState.Error
}

/** User and composition events accepted by [BrowseViewModel]; the screen never calls repositories. */
sealed interface BrowseIntent {
    object Entered : BrowseIntent
    data class OpenCluster(val clusterId: Int) : BrowseIntent
    data class WatchCluster(val clusterId: Int) : BrowseIntent
    object Reload : BrowseIntent
    object LoadNextPage : BrowseIntent
    object RetryNextPage : BrowseIntent
    data class ChangeSortType(val value: SortType) : BrowseIntent
    data class ChangeCategory(val value: OpportunityCategoryFilter) : BrowseIntent
}

/** One-shot actions handled by the navigation host, separate from replayable [BrowseState]. */
sealed interface BrowseEffect {
    data class ShowError(val error: OpportunityUiError) : BrowseEffect
    data class NavigateToCluster(val clusterId: Int) : BrowseEffect
}
