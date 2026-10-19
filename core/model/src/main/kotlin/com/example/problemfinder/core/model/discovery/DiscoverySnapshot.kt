package com.example.problemfinder.core.model.discovery

/**
 * Account-scoped cached dashboard. cacheReady becomes true after the local read; hasSynced records
 * whether a dashboard was stored, even if empty. A null userId represents the signed-out view.
 */
data class DiscoverySnapshot(
    val userId: String?,
    val dashboard: DiscoveryDashboard,
    val hasSynced: Boolean = false,
    val cacheReady: Boolean = true
)
