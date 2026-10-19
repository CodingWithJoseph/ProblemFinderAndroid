package com.example.problemfinder.data.opportunities.api

interface OpportunityActionsRepository {
    suspend fun setWatched(userId: String, clusterId: Int, watched: Boolean)
}
