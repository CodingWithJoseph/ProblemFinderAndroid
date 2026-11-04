package com.example.problemfinder.data.opportunities.api

import com.example.problemfinder.core.model.details.OpportunityDetailsSnapshot
import kotlinx.coroutines.flow.Flow

interface OpportunityDetailsRepository {
    fun observeDetails(clusterId: Int): Flow<OpportunityDetailsSnapshot>
    suspend fun loadPost(clusterId: Int, postId: String, userId: String): com.example.problemfinder.core.model.details.OpportunityEvidence?
    suspend fun refreshDetails(clusterId: Int)
    suspend fun loadNextEvidencePage(clusterId: Int)
}
