package com.example.problemfinder.data.opportunities.api

import com.example.problemfinder.core.model.search.OpportunitySearchDraft
import com.example.problemfinder.core.model.search.OpportunitySearchQuery
import com.example.problemfinder.core.model.search.OpportunitySearchSnapshot
import kotlinx.coroutines.flow.Flow

/**
 * TODO [SEARCH-3] Keep query identity and account ownership through observation, refresh and paging.
 * Repositories own stored pagination progress. Screens should never calculate/send offsets.
 * Preparing an assistant draft is separate from executing it with refreshSearch.
 */
interface OpportunitySearchRepository {
    fun observeSearch(query: OpportunitySearchQuery): Flow<OpportunitySearchSnapshot>
    suspend fun refreshSearch(query: OpportunitySearchQuery)
    suspend fun loadNextSearchPage(query: OpportunitySearchQuery)
    suspend fun prepareDraft(message: String, currentDraft: OpportunitySearchQuery?): OpportunitySearchDraft
}
