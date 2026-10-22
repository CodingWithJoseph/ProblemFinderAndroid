package com.example.problemfinder.data.opportunities.impl.db.dao

import androidx.room3.Query
import androidx.room3.Dao
import androidx.room3.Upsert
import com.example.problemfinder.data.opportunities.impl.model.entity.OpportunityEntity

/** Shared card content: writes invalidate both Browse and Discovery queries that join these rows. */
@Dao
interface OpportunityDao {
    /** Updates an existing account-owned card; returns the affected row count (zero if uncached). */
    @Query("UPDATE opportunities SET isWatched = :watched WHERE userId = :userId AND clusterId = :clusterId")
    suspend fun setWatched(userId: String, clusterId: Int, watched: Boolean): Int

    /** Updates content in place so foreign-key memberships and their positions survive refreshes. */
    @Upsert
    suspend fun upsertOpportunities(opportunities: List<OpportunityEntity>)
}
