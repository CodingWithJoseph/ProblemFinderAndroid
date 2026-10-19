package com.example.problemfinder.feature.opportunities.impl.discovery

import com.example.problemfinder.core.model.opportunity.OpportunityCategory
import com.example.problemfinder.core.model.opportunity.Opportunity
import com.example.problemfinder.core.model.discovery.DiscoveryCollection
import com.example.problemfinder.core.mvi.RequestState
import com.example.problemfinder.feature.opportunities.impl.util.OpportunityUiError

data class DiscoveryState(
    val userId: String? = null,
    val featuredCluster: Opportunity? = null,
    val collections: List<DiscoveryCollection> = emptyList(),
    val hasSynced: Boolean = false,
    val cacheReady: Boolean = false,
    val observationRequest: RequestState<Unit, OpportunityUiError> = RequestState.Loading,
    val refreshRequestState: RequestState<Unit, OpportunityUiError> = RequestState.Idle,
    val pendingClusterIds: Set<Int> = emptySet(),
) {
    val hasContent: Boolean
        get() = featuredCluster != null ||
            collections.any { it.clusters.isNotEmpty() }

    val isRefreshing: Boolean
        get() = refreshRequestState == RequestState.Loading

    val contentError: OpportunityUiError?
        get() {
            val observation = observationRequest
            if (observation is RequestState.Error) {
                return observation.error
            }

            val refresh = refreshRequestState
            return if (refresh is RequestState.Error) {
                refresh.error
            } else {
                null
            }
        }

    val canRefresh: Boolean
        get() = !isRefreshing && (
            observationRequest is RequestState.Error ||
                (
                    cacheReady &&
                        userId != null &&
                        observationRequest is RequestState.Success
                    )
            )
}

sealed interface DiscoveryIntent {
    data object Entered : DiscoveryIntent
    data object Reload : DiscoveryIntent

    data class OpenCluster(val clusterId: Int): DiscoveryIntent
    data class WatchCluster(val clusterId: Int): DiscoveryIntent
    data class SeeAll(val clusterCategory: OpportunityCategory): DiscoveryIntent
}

sealed interface DiscoveryEffect {
    data class NavigateToCluster(val clusterId: Int): DiscoveryEffect
    data class NavigateToBrowse(val clusterCategory: OpportunityCategory): DiscoveryEffect
    data class ShowError(val error: OpportunityUiError): DiscoveryEffect
}
