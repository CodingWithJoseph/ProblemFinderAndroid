package com.example.problemfinder.data.opportunities.impl.local

import androidx.room3.withWriteTransaction
import com.example.problemfinder.core.model.discovery.DiscoveryDashboard
import com.example.problemfinder.core.model.discovery.DiscoverySnapshot
import com.example.problemfinder.data.opportunities.impl.db.dao.OpportunityDao
import com.example.problemfinder.data.opportunities.impl.db.dao.DiscoveryDao
import com.example.problemfinder.data.opportunities.impl.db.OpportunitiesDatabase
import com.example.problemfinder.data.opportunities.impl.model.entity.DiscoveryCollectionItemEntity
import com.example.problemfinder.data.opportunities.impl.model.entity.DiscoveryCollectionEntity
import com.example.problemfinder.data.opportunities.impl.model.entity.DiscoveryDashboardEntity
import com.example.problemfinder.data.opportunities.impl.util.toDiscoverySnapshot
import com.example.problemfinder.data.opportunities.impl.util.toModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Persists dashboard arrangement while reusing the opportunity content also displayed by Browse. */
class DiscoveryLocalDataSource @Inject constructor(
    private val database: OpportunitiesDatabase,
    private val opportunityDao: OpportunityDao,
    private val discoveryDao: DiscoveryDao
) {

    /** Emits complete joined snapshots, suppressing duplicates when database changes leave them unchanged. */
    fun observeDashboard(
        userId: String,
    ): Flow<DiscoverySnapshot> =
        discoveryDao.observeDashboard(userId)
            .map { rows -> rows.toDiscoverySnapshot(userId) }
            .distinctUntilChanged()

    /** Atomically replaces one account's dashboard, including a successfully fetched empty result. */
    suspend fun replaceDashboard(
        userId: String,
        dashboard: DiscoveryDashboard
    ) {
        // Store each card once even when it appears as featured and in one or more collections.
        val clusters = buildList {
            dashboard.featuredCluster?.let {
                add(it)
            }

            dashboard.collections.forEach {
                addAll(it.clusters)
            }
        }.distinctBy { it.id }.map { it.toModel(userId) }

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

        // Membership uses the owning collection's category, which can differ from the card's category.
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
            // Cascading deletes remove old collection items, but leave shared Browse content intact.
            discoveryDao.clearCollections(userId)
            discoveryDao.insertCollections(collections)
            discoveryDao.insertCollectionItems(items)
        }
    }
}
