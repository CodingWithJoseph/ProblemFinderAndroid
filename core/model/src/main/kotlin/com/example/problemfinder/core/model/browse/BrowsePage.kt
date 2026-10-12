package com.example.problemfinder.core.model.browse

import com.example.problemfinder.core.model.discovery.DiscoveryCluster

data class BrowsePage(
    val clusters: List<DiscoveryCluster>,
    val nextCursor: String? = null
)
