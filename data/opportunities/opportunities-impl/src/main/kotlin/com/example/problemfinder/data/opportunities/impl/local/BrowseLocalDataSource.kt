package com.example.problemfinder.data.opportunities.impl.local

import com.example.problemfinder.data.opportunities.impl.db.dao.OpportunityDao
import androidx.room3.withWriteTransaction
import com.example.problemfinder.core.common.shared.OpportunityCategoryFilter
import com.example.problemfinder.core.common.shared.SortType
import com.example.problemfinder.core.model.browse.BrowsePage
import com.example.problemfinder.core.model.browse.BrowseSnapshot
import com.example.problemfinder.data.opportunities.impl.model.entity.BrowseQueryEntity
import com.example.problemfinder.data.opportunities.impl.model.entity.BrowseQueryItemEntity
import com.example.problemfinder.data.opportunities.impl.db.dao.BrowseDao
import com.example.problemfinder.data.opportunities.impl.db.OpportunitiesDatabase
import com.example.problemfinder.data.opportunities.impl.util.toModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

/**
 * Stores query membership/order separately from shared opportunity content. A query is identified
 * by account, category, and sort; refreshing one query leaves other queries' memberships intact.
 */
class BrowseLocalDataSource @Inject constructor(
    private val database: OpportunitiesDatabase,
    private val dao: BrowseDao,
    private val opportunityDao: OpportunityDao
) {

    /** Replaces all loaded membership for this query, including pages appended to its old first page. */
    suspend fun replaceFirstPage(
        userId: String,
        category: OpportunityCategoryFilter,
        sortType: SortType,
        page: BrowsePage
    ) {
        val categoryKey = category.name
        val sortKey = sortType.name
        val clusters = page.clusters.distinctBy { it.id }

        val clusterEntities = clusters.map {
            it.toModel(userId)
        }

        val query = BrowseQueryEntity(
            userId = userId,
            category = categoryKey,
            sort = sortKey,
            nextCursor = page.nextCursor,
            hasSynced = true
        )

        val items = clusters.mapIndexed { index, cluster ->
            BrowseQueryItemEntity(
                userId = userId,
                category = categoryKey,
                sort = sortKey,
                clusterId = cluster.id,
                position = index
            )
        }

        // Content, order, and cursor become visible together, or roll back together on failure.
        database.withWriteTransaction {
            dao.upsertQuery(query)
            opportunityDao.upsertOpportunities(clusterEntities)
            dao.clearQueryItems(userId, categoryKey, sortKey)
            dao.insertQueryItems(items)
        }
    }

    /** Reconstructs the query snapshot; a saved empty query still has hasSynced=true. */
    fun observeBrowse(
        userId: String,
        category: OpportunityCategoryFilter,
        sortType: SortType
    ): Flow<BrowseSnapshot> =
        dao.observeBrowse(userId, category.name, sortType.name)
            .map { rows ->
                val query = rows.firstOrNull()?.query

                BrowseSnapshot(
                    clusters = rows.mapNotNull {
                        it.cluster?.toModel()
                    },
                    nextCursor = query?.nextCursor,
                    hasSynced = query?.hasSynced ?: false
                )
            }
            .distinctUntilChanged()


    /**
     * Applies a page only if [expectedCursor] still matches the stored query. Returns false for a
     * stale response without changing content; duplicate cards update content but keep their position.
     */
    suspend fun appendPage(
        userId: String,
        category: OpportunityCategoryFilter,
        sortType: SortType,
        expectedCursor: String,
        page: BrowsePage
    ): Boolean {
        val categoryKey = category.name
        val sortKey = sortType.name
        val clusters = page.clusters.distinctBy { it.id }
        val clusterEntities = clusters.map { it.toModel(userId) }

        return database.withWriteTransaction {
            val query = dao.getQuery(userId, categoryKey, sortKey)

            // Recheck inside the write transaction, even when the repository already checked.
            if (
                query == null ||
                !query.hasSynced ||
                query.nextCursor != expectedCursor
            ) {
                return@withWriteTransaction false
            }

            require(page.nextCursor != expectedCursor) {
                "Browse cursor did not advance"
            }

            val existingItems =
                dao.getQueryItems(userId, categoryKey, sortKey)

            val existingIds = existingItems.mapTo(mutableSetOf()) {
                it.clusterId
            }

            val nextPosition =
                (existingItems.maxOfOrNull { it.position } ?: -1) + 1

            val newItems = clusters
                .filterNot { it.id in existingIds }
                .mapIndexed { index, cluster ->
                    BrowseQueryItemEntity(
                        userId = userId,
                        category = categoryKey,
                        sort = sortKey,
                        clusterId = cluster.id,
                        position = nextPosition + index
                    )
                }

            // Even an empty or duplicate-only page must advance the cursor to avoid refetching it.
            opportunityDao.upsertOpportunities(clusterEntities)
            dao.insertQueryItems(newItems)
            dao.upsertQuery(query.copy(nextCursor = page.nextCursor))

            true
        }
    }

    /** Reads pagination metadata for exactly one account/filter/sort combination. */
    suspend fun getQuery(
        userId: String,
        category: OpportunityCategoryFilter,
        sortType: SortType
    ): BrowseQueryEntity? = dao.getQuery(userId, category.name, sortType.name)
}
