package com.example.problemfinder.data.opportunities.impl.model.entity

import androidx.room3.Embedded

/** Left-join projection: query metadata survives even when there is no card or position. */
data class BrowseCacheRow(
    @Embedded(prefix = "query_")
    val query: BrowseQueryEntity,

    @Embedded(prefix = "cluster_")
    val cluster: OpportunityEntity?,

    val position: Int?
)
