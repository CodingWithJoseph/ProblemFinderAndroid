package com.example.problemfinder.data.opportunities.impl.db

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Upsert
import com.example.problemfinder.data.opportunities.impl.model.entity.DiscoveryCacheRow
import com.example.problemfinder.data.opportunities.impl.model.entity.DiscoveryCollectionItemEntity
import com.example.problemfinder.data.opportunities.impl.model.entity.DiscoveryCollectionEntity
import com.example.problemfinder.data.opportunities.impl.model.entity.DiscoveryDashboardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DiscoveryDao {
    @Query(
        """
    SELECT
        d.userId AS dashboard_userId,
        d.featuredClusterId AS dashboard_featuredClusterId,
        d.hasSynced AS dashboard_hasSynced,

        f.userId AS featured_userId,
        f.clusterId AS featured_clusterId,
        f.name AS featured_name,
        f.summary AS featured_summary,
        f.problemStatement AS featured_problemStatement,
        f.postCount AS featured_postCount,
        f.category AS featured_category,
        f.isWatched AS featured_isWatched,
        f.dateIso AS featured_dateIso,

        co.userId AS collection_userId,
        co.category AS collection_category,

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
    FROM discovery_dashboard AS d
    LEFT JOIN opportunities AS f
        ON f.userId = d.userId
        AND f.clusterId = d.featuredClusterId
    LEFT JOIN discovery_collections AS co
        ON co.userId = d.userId
    LEFT JOIN discovery_collection_items AS i
        ON i.userId = co.userId
        AND i.category = co.category
    LEFT JOIN opportunities AS c
        ON c.userId = i.userId
        AND c.clusterId = i.clusterId
    WHERE d.userId = :userId
    ORDER BY
        CASE co.category
            WHEN 'build_and_tooling' THEN 0
            WHEN 'data_and_apis' THEN 1
            WHEN 'workflow_and_automation' THEN 2
            ELSE 3
        END,
        i.position ASC
    """
    )
    fun observeDashboard(userId: String): Flow<List<DiscoveryCacheRow>>

    @Upsert
    suspend fun upsertDashboard(dashboardEntity: DiscoveryDashboardEntity)

    @Insert
    suspend fun insertCollections(collections: List<DiscoveryCollectionEntity>)

    @Insert
    suspend fun insertCollectionItems(items: List<DiscoveryCollectionItemEntity>)

    @Query("DELETE FROM discovery_collections where userId = :userId")
    suspend fun clearCollections(userId: String)
}
