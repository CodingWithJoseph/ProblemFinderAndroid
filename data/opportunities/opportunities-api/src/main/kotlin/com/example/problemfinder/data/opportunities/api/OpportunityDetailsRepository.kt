package com.example.problemfinder.data.opportunities.api

import com.example.problemfinder.core.model.details.OpportunityDetailsSnapshot
import kotlinx.coroutines.flow.Flow

/**
 * Details use cases depend on this contract; the feature never imports Ktor or Room.
 * TODO [DETAILS-3] Implement these operations in OpportunityDetailsRepositoryImpl.
 * observeDetails owns displayed content. Refresh and pagination only update persistent data.
 */
interface OpportunityDetailsRepository {
    fun observeDetails(clusterId: Int): Flow<OpportunityDetailsSnapshot>
    suspend fun refreshDetails(clusterId: Int)
    suspend fun loadNextEvidencePage(clusterId: Int)
}
