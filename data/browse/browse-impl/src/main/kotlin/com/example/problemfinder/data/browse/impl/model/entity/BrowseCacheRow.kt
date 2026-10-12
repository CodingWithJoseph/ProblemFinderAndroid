package com.example.problemfinder.data.browse.impl.model.entity

import androidx.room3.Embedded

data class BrowseCacheRow(
    @Embedded(prefix = "query_")
    val query: BrowseQueryEntity,

    @Embedded(prefix = "cluster_")
    val cluster: BrowseClusterEntity?,

    val position: Int?
)
