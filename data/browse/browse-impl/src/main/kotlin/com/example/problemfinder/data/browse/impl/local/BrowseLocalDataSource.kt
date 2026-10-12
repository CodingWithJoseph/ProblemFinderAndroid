package com.example.problemfinder.data.browse.impl.local

import androidx.room3.withWriteTransaction
import com.example.problemfinder.core.common.shared.Category
import com.example.problemfinder.core.common.shared.SortType
import com.example.problemfinder.core.model.browse.BrowsePage
import com.example.problemfinder.core.model.browse.BrowseSnapshot
import com.example.problemfinder.data.browse.impl.model.entity.BrowseQueryEntity
import com.example.problemfinder.data.browse.impl.model.entity.BrowseQueryItemEntity
import com.example.problemfinder.data.browse.impl.util.toEntity
import com.example.problemfinder.data.browse.impl.util.toModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

class BrowseLocalDataSource @Inject constructor(
    private val database: BrowseDatabase,
    private val dao: BrowseDao
) {

    suspend fun replaceFirstPage(
        userId: String,
        category: Category,
        sortType: SortType,
        page: BrowsePage
    ) {
        val categoryKey = category.name
        val sortKey = sortType.name
        val clusters = page.clusters.distinctBy { it.id }

        val clusterEntities = clusters.map {
            it.toEntity(userId)
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

        database.withWriteTransaction {
            dao.upsertQuery(query)
            dao.upsertClusters(clusterEntities)
            dao.clearQueryItems(userId, categoryKey, sortKey)
            dao.insertQueryItems(items)
        }
    }

    fun observeBrowse(
        userId: String,
        category: Category,
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


    suspend fun appendPage(
        userId: String,
        category: Category,
        sortType: SortType,
        expectedCursor: String,
        page: BrowsePage
    ): Boolean {
        val categoryKey = category.name
        val sortKey = sortType.name
        val clusters = page.clusters.distinctBy { it.id }
        val clusterEntities = clusters.map { it.toEntity(userId) }

        return database.withWriteTransaction {
            val query = dao.getQuery(userId, categoryKey, sortKey)

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

            dao.upsertClusters(clusterEntities)
            dao.insertQueryItems(newItems)
            dao.upsertQuery(query.copy(nextCursor = page.nextCursor))

            true
        }
    }

    suspend fun getQuery(
        userId: String,
        category: Category,
        sortType: SortType
    ): BrowseQueryEntity? = dao.getQuery(userId, category.name, sortType.name)
}
