package com.example.problemfinder.data.opportunities.impl.model.dto

import kotlinx.serialization.Serializable


/** Server-defined grouping; its category is independent of each card's own classification. */
@Serializable
data class DiscoveryCollectionDto(
    val category: String,
    val clusters: List<OpportunityDto>
)


/** Full dashboard response; null featured and empty collections are valid successful content. */
@Serializable
data class DiscoveryDashboardDto(
    val featured: OpportunityDto?,
    val collections: List<DiscoveryCollectionDto>
)
