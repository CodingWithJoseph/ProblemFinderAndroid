package com.example.problemfinder.core.model.discovery

import kotlin.time.Instant

data class DiscoveryCluster(
    val id: Int,
    val name: String?,
    val summary: String?,
    val problemStatement: String?,
    val postCount: Int,
    val isWatched: Boolean,
    val date: Instant?
)
