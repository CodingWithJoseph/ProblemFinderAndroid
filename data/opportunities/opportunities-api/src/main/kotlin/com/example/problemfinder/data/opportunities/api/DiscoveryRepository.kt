package com.example.problemfinder.data.opportunities.api

import com.example.problemfinder.core.model.discovery.DiscoverySnapshot
import kotlinx.coroutines.flow.Flow

interface DiscoveryRepository {
    fun observeDiscovery(): Flow<DiscoverySnapshot>
    suspend fun refresh()
}
