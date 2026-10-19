package com.example.problemfinder.data.opportunities.impl.db

import androidx.room3.Query
import androidx.room3.Dao
import androidx.room3.Upsert
import com.example.problemfinder.data.opportunities.impl.model.entity.OpportunityEntity

@Dao
interface OpportunityDao {
    @Query("UPDATE opportunities SET isWatched = :watched WHERE userId = :userId AND clusterId = :clusterId")
    suspend fun setWatched(userId: String, clusterId: Int, watched: Boolean): Int

    @Upsert
    suspend fun upsertOpportunities(opportunities: List<OpportunityEntity>)
}
