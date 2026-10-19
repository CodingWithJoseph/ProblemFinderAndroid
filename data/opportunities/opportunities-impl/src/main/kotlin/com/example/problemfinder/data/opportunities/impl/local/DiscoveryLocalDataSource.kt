package com.example.problemfinder.data.opportunities.impl.local

import androidx.room3.withWriteTransaction
import com.example.problemfinder.core.model.discovery.DiscoveryDashboard
import com.example.problemfinder.core.model.discovery.DiscoverySnapshot
import com.example.problemfinder.data.opportunities.impl.db.OpportunityDao
import com.example.problemfinder.data.opportunities.impl.db.DiscoveryDao
import com.example.problemfinder.data.opportunities.impl.db.OpportunitiesDatabase
import com.example.problemfinder.data.opportunities.impl.model.entity.DiscoveryCollectionItemEntity
import com.example.problemfinder.data.opportunities.impl.model.entity.DiscoveryCollectionEntity
import com.example.problemfinder.data.opportunities.impl.model.entity.DiscoveryDashboardEntity
import com.example.problemfinder.data.opportunities.impl.util.toDiscoverySnapshot
import com.example.problemfinder.data.opportunities.impl.util.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DiscoveryLocalDataSource @Inject constructor(
    private val database: OpportunitiesDatabase,
    private val opportunityDao: OpportunityDao,
    private val discoveryDao: DiscoveryDao
) {

    fun observeDashboard(
        userId: String,
    ): Flow<DiscoverySnapshot> =
        discoveryDao.observeDashboard(userId)
            .map { rows -> rows.toDiscoverySnapshot(userId) }
            .distinctUntilChanged()

    suspend fun replaceDashboard(
        userId: String,
        dashboard: DiscoveryDashboard
    ) {
        val clusters = buildList {
            dashboard.featuredCluster?.let {
                add(it)
            }

            dashboard.collections.forEach {
                addAll(it.clusters)
            }
        }.distinctBy { it.id }.map { it.toEntity(userId) }

        val dashboardEntity = DiscoveryDashboardEntity(
            userId = userId,
            featuredClusterId = dashboard.featuredCluster?.id,
            hasSynced = true
        )

        val collections = dashboard.collections.map {
            DiscoveryCollectionEntity(
                userId = userId,
                category = it.category.name.lowercase(),
            )
        }

        val items = dashboard.collections.flatMap {
            it.clusters.mapIndexed { index, cluster ->
                DiscoveryCollectionItemEntity(
                    userId = userId,
                    category = it.category.name.lowercase(),
                    clusterId = cluster.id,
                    position = index
                )
            }
        }

        database.withWriteTransaction {
            opportunityDao.upsertOpportunities(clusters)
            discoveryDao.upsertDashboard(dashboardEntity)
            discoveryDao.clearCollections(userId)
            discoveryDao.insertCollections(collections)
            discoveryDao.insertCollectionItems(items)
        }
    }
}
