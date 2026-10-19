package com.example.problemfinder.data.opportunities.impl.model.entity

import androidx.room3.Embedded

/**
 * Flattened dashboard join: dashboard/featured data repeats per item. Nullable joined values retain
 * featured-only dashboards and empty collection headers for reconstruction by the cache mapper.
 */
data class DiscoveryCacheRow(
    @Embedded(prefix = "dashboard_")
    val dashboard: DiscoveryDashboardEntity,

    @Embedded(prefix = "featured_")
    val featured: OpportunityEntity?,

    @Embedded(prefix = "collection_")
    val collection: DiscoveryCollectionEntity?,

    @Embedded(prefix = "cluster_")
    val cluster: OpportunityEntity?,

    val position: Int?
)
