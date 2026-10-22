package com.example.problemfinder.data.opportunities.impl.db.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Upsert
import com.example.problemfinder.data.opportunities.impl.model.entity.BrowseCacheRow
import com.example.problemfinder.data.opportunities.impl.model.entity.BrowseQueryEntity
import com.example.problemfinder.data.opportunities.impl.model.entity.BrowseQueryItemEntity
import kotlinx.coroutines.flow.Flow

/** Query metadata and ordered memberships; multi-table writes are grouped by BrowseLocalDataSource. */
@Dao
interface BrowseDao {
    /**
     * Left joins retain metadata for a successfully synced empty query. Every join includes userId
     * so the same cluster ID in another account cannot supply content or watch state.
     */
    @Query(
        """
        SELECT
            q.userId AS query_userId,
            q.category AS query_category,
            q.sort AS query_sort,
            q.nextCursor AS query_nextCursor,
            q.hasSynced AS query_hasSynced,
            c.userId AS cluster_userId,
            c.clusterId AS cluster_clusterId,
            c.name AS cluster_name,
            c.summary AS cluster_summary,
            c.problemStatement AS cluster_problemStatement,
            c.postCount AS cluster_postCount,
            c.category AS cluster_category,
            c.isWatched AS cluster_isWatched,
            c.dateIso AS cluster_dateIso,
            i.position AS position
        FROM browse_queries AS q
        LEFT JOIN browse_query_items AS i
            ON i.userId = q.userId
            AND i.category = q.category
            AND i.sort = q.sort
        LEFT JOIN opportunities AS c
            ON c.userId = i.userId
            AND c.clusterId = i.clusterId
        WHERE q.userId = :userId
            AND q.category = :category
            AND q.sort = :sort
        ORDER BY i.position ASC
        """
    )
    fun observeBrowse(
        userId: String,
        category: String,
        sort: String
    ): Flow<List<BrowseCacheRow>>

    @Query(
        """
    SELECT * FROM browse_queries
    WHERE userId = :userId
        AND category = :category
        AND sort = :sort
    """
    )
    suspend fun getQuery(
        userId: String,
        category: String,
        sort: String
    ): BrowseQueryEntity?

    @Query(
        """
    SELECT * FROM browse_query_items
    WHERE userId = :userId
        AND category = :category
        AND sort = :sort
    ORDER BY position ASC
    """
    )
    suspend fun getQueryItems(
        userId: String,
        category: String,
        sort: String
    ): List<BrowseQueryItemEntity>

    /** Inserts/updates metadata without deleting the memberships that refer to it. */
    @Upsert
    suspend fun upsertQuery(
        query: BrowseQueryEntity
    )

    @Insert
    suspend fun insertQueryItems(
        items: List<BrowseQueryItemEntity>
    )

    /** Deletes this query's arrangement, retaining shared opportunity content and other queries. */
    @Query(
        """
    DELETE FROM browse_query_items
    WHERE userId = :userId
        AND category = :category
        AND sort = :sort
    """
    )
    suspend fun clearQueryItems(
        userId: String,
        category: String,
        sort: String
    )
}
