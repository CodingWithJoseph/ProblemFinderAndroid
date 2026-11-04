package com.example.problemfinder.data.opportunities.impl.db.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Upsert
import com.example.problemfinder.data.opportunities.impl.model.entity.OpportunitySearchCacheRow
import com.example.problemfinder.data.opportunities.impl.model.entity.OpportunitySearchQueryEntity
import com.example.problemfinder.data.opportunities.impl.model.entity.OpportunitySearchQueryItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OpportunitySearchDao {
    @Query("""
        SELECT q.total, q.nextOffset, c.*
        FROM search_queries q
        LEFT JOIN search_query_items i ON i.userId = q.userId AND i.queryKey = q.queryKey
        LEFT JOIN opportunities c ON c.userId = i.userId AND c.clusterId = i.clusterId
        WHERE q.userId = :userId AND q.queryKey = :key
        ORDER BY i.position
    """)
    fun observe(userId: String, key: String): Flow<List<OpportunitySearchCacheRow>>

    @Query("SELECT nextOffset FROM search_queries WHERE userId = :userId AND queryKey = :key")
    suspend fun nextOffset(userId: String, key: String): Int?

    @Query("SELECT clusterId FROM search_query_items WHERE userId = :userId AND queryKey = :key ORDER BY position")
    suspend fun ids(userId: String, key: String): List<Int>

    @Query("DELETE FROM search_query_items WHERE userId = :userId AND queryKey = :key")
    suspend fun clear(userId: String, key: String)

    @Upsert suspend fun upsert(query: OpportunitySearchQueryEntity)
    @Insert suspend fun insert(items: List<OpportunitySearchQueryItemEntity>)
}
