package com.example.problemfinder.data.opportunities.api

import com.example.problemfinder.core.common.shared.OpportunityCategoryFilter
import com.example.problemfinder.core.common.shared.SortType
import com.example.problemfinder.core.model.browse.BrowsePage
import com.example.problemfinder.core.model.browse.BrowseSnapshot
import kotlinx.coroutines.flow.Flow

/**
 * Account-aware access to Browse results. Observation reads the cache; explicit operations fetch
 * pages and update that cache, which then emits the new snapshot to existing observers.
 */
interface BrowseRepository {

    /** Follows the signed-in account and the selected category/sort without making a request. */
    fun observeBrowse(category: OpportunityCategoryFilter, sortType: SortType): Flow<BrowseSnapshot>
    /** Replaces this query's loaded pages with a fresh first page; failures retain cached results. */
    suspend fun refresh(category: OpportunityCategoryFilter, sortType: SortType)
    /** Requires a usable session, then appends by cursor; skips unfetched queries and completed lists. */
    suspend fun loadNextPage(category: OpportunityCategoryFilter, sortType: SortType
    )
}
