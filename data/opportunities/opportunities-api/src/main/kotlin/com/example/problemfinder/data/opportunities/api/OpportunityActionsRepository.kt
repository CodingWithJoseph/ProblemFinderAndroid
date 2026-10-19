package com.example.problemfinder.data.opportunities.api

/** Changes pipeline membership shared by Browse and Discovery opportunity cards. */
interface OpportunityActionsRepository {
    /**
     * Confirms membership with the server before updating cached cards. [userId] is the account
     * that owned the tapped card; a stale account cancels the operation instead of targeting a new one.
     */
    suspend fun setWatched(userId: String, clusterId: Int, watched: Boolean)
}
