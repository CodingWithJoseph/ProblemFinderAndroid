package com.example.problemfinder.data.opportunities.api

import com.example.problemfinder.core.model.search.OpportunitySearchDraft
import com.example.problemfinder.core.model.search.OpportunitySearchQuery
import com.example.problemfinder.core.model.search.OpportunitySearchSnapshot
import kotlinx.coroutines.flow.Flow

/**
 * Keep query identity and account ownership through observation, refresh and paging.
 * Repositories own stored pagination progress. Screens should never calculate/send offsets.
 * Preparing an assistant draft is separate from executing it with refreshSearch.
 */
interface OpportunitySearchRepository {
    suspend fun filterOptions(): com.example.problemfinder.core.model.search.OpportunitySearchOptions
    fun observeSearch(query: OpportunitySearchQuery): Flow<OpportunitySearchSnapshot>
    suspend fun refreshSearch(query: OpportunitySearchQuery)
    suspend fun loadNextSearchPage(query: OpportunitySearchQuery)
    suspend fun prepareDraft(message: String, currentDraft: OpportunitySearchQuery?): OpportunitySearchDraft
}
