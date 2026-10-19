package com.example.problemfinder.data.opportunities.api

import com.example.problemfinder.core.model.discovery.DiscoverySnapshot
import kotlinx.coroutines.flow.Flow

/** Exposes the signed-in account's cached dashboard separately from network refresh requests. */
interface DiscoveryRepository {
    /** Emits cache snapshots and switches data sources when the current account changes. */
    fun observeDiscovery(): Flow<DiscoverySnapshot>
    /** Replaces the dashboard after a successful fetch; failures leave the existing cache intact. */
    suspend fun refresh()
}
