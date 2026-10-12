package com.example.problemfinder.core.model.browse

import com.example.problemfinder.core.model.discovery.DiscoveryCluster

data class BrowseSnapshot(
    val userId: String? = null,
    val clusters: List<DiscoveryCluster> = emptyList(),
    val nextCursor: String? = null,
    val hasSynced: Boolean = false,
    val cacheReady: Boolean = true
)
