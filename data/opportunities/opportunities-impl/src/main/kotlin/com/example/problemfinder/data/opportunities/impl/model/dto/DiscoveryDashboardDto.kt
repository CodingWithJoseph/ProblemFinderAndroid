package com.example.problemfinder.data.opportunities.impl.model.dto

import kotlinx.serialization.Serializable


@Serializable
data class DiscoveryCollectionDto(
    val category: String,
    val clusters: List<OpportunityDto>
)


@Serializable
data class DiscoveryDashboardDto(
    val featured: OpportunityDto?,
    val collections: List<DiscoveryCollectionDto>
)
