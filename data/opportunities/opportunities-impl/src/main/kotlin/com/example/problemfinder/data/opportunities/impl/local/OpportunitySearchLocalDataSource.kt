package com.example.problemfinder.data.opportunities.impl.local

import androidx.room3.withWriteTransaction
import com.example.problemfinder.core.model.opportunity.OpportunityCategory
import com.example.problemfinder.core.model.search.OpportunitySearchPage
import com.example.problemfinder.core.model.search.OpportunitySearchQuery
import com.example.problemfinder.core.model.search.OpportunitySearchSnapshot
import com.example.problemfinder.data.opportunities.impl.db.OpportunitiesDatabase
import com.example.problemfinder.data.opportunities.impl.model.entity.OpportunitySearchQueryEntity
import com.example.problemfinder.data.opportunities.impl.model.entity.OpportunitySearchQueryItemEntity
import com.example.problemfinder.data.opportunities.impl.util.cacheKey
import com.example.problemfinder.data.opportunities.impl.util.toModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class OpportunitySearchLocalDataSource @Inject constructor(private val database: OpportunitiesDatabase) {
    private val dao = database.searchDao()

    fun observeSearch(userId: String, query: OpportunitySearchQuery) =
        dao.observe(userId, query.cacheKey()).map { rows ->
            val metadata = rows.firstOrNull()
            OpportunitySearchSnapshot(
                query = query,
                userId = userId,
                items = rows.mapNotNull { it.card?.toModel() },
                total = metadata?.total ?: 0,
                nextOffset = metadata?.nextOffset,
                cacheReady = true,
                hasSynced = metadata != null,
            )
        }.distinctUntilChanged()

    suspend fun nextOffset(userId: String, query: OpportunitySearchQuery): Int? =
        dao.nextOffset(userId, query.cacheKey())

    suspend fun storePage(userId: String, query: OpportunitySearchQuery, page: OpportunitySearchPage, replace: Boolean) {
        val key = query.cacheKey()
        val cards = page.items.distinctBy { it.id }
        database.withWriteTransaction {
            val opportunityDao = database.opportunityDao()
            val rows = cards.map { card ->
                val entity = card.toModel(userId)
                // Older search responses omit category. Retain the category of an existing card.
                if (card.category == OpportunityCategory.UNCATEGORIZED) {
                    opportunityDao.find(userId, card.id)?.let { entity.copy(category = it.category) } ?: entity
                } else entity
            }
            opportunityDao.upsertOpportunities(rows)
            val existing = if (replace) {
                dao.clear(userId, key)
                emptyList()
            } else dao.ids(userId, key)
            val seen = existing.toHashSet()
            dao.upsert(OpportunitySearchQueryEntity(userId, key, page.total, page.nextOffset))
            dao.insert(cards.filter { seen.add(it.id) }.mapIndexed { index, card ->
                OpportunitySearchQueryItemEntity(userId, key, card.id, existing.size + index)
            })
        }
    }
}
