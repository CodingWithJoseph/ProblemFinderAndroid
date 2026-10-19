package com.example.problemfinder.core.model.discovery

data class DiscoverySnapshot(
    val userId: String?,
    val dashboard: DiscoveryDashboard,
    val hasSynced: Boolean = false,
    val cacheReady: Boolean = true
)
