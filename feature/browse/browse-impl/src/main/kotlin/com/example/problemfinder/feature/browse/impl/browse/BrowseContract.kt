package com.example.problemfinder.feature.browse.impl.browse

import com.example.problemfinder.core.common.shared.Category
import com.example.problemfinder.core.common.shared.SortType
import com.example.problemfinder.core.model.discovery.DiscoveryCluster
import com.example.problemfinder.core.mvi.RequestState
import com.example.problemfinder.feature.browse.impl.BrowseUIError

data class BrowseState(
    val category: Category = Category.ALL,
    val sort: SortType = SortType.NEWEST,
    val userId: String? = null,
    val data: List<DiscoveryCluster> = emptyList(),
    val cursor: String? = null,
    val hasSynced: Boolean = false,
    val cacheReady: Boolean = false,
    val observationRequest: RequestState<Unit, BrowseUIError> = RequestState.Loading,
    val refreshRequest: RequestState<Unit, BrowseUIError> = RequestState.Idle,
    val pageRequest: RequestState<Unit, BrowseUIError> = RequestState.Idle,
) {
    val isBusy: Boolean
        get() = refreshRequest == RequestState.Loading ||
            pageRequest == RequestState.Loading

    val contentError: BrowseUIError?
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

    val canAutoLoadNextPage: Boolean
        get() = canLoadNextPage && pageRequest !is RequestState.Error
}

sealed interface BrowseIntent {
    object Entered : BrowseIntent
    object Reload : BrowseIntent
    object LoadNextPage : BrowseIntent
    object RetryNextPage : BrowseIntent
    data class ChangeSortType(val value: SortType) : BrowseIntent
    data class ChangeCategory(val value: Category) : BrowseIntent
}

sealed interface BrowseEffect
