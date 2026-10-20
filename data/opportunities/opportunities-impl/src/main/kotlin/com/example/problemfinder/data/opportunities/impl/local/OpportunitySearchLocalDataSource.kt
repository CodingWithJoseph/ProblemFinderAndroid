package com.example.problemfinder.data.opportunities.impl.local

import com.example.problemfinder.core.model.search.OpportunitySearchPage
import com.example.problemfinder.core.model.search.OpportunitySearchQuery
import com.example.problemfinder.core.model.search.OpportunitySearchSnapshot
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class OpportunitySearchLocalDataSource @Inject constructor() {
    fun observeSearch(userId: String, query: OpportunitySearchQuery): Flow<OpportunitySearchSnapshot> {
        // TODO [SEARCH-2C] Turn this account/query's completed DAO reads into a snapshot.
        // 1. Inject the registered DAO/database. Derive cacheKey from the normalized SUBMITTED query.
        // 2. Observe its metadata/membership/shared-card join and map shared entities back to Opportunity.
        // 3. Reassemble unique result IDs in stored position order; include stored total/nextOffset/hasSynced.
        // 4. Set cacheReady=true after the first actual read. No metadata means never synced; saved metadata
        //    with an empty membership set can mean successfully zero results. Do not merge those states.
        // 5. Include the same query and userId on the snapshot so the ViewModel can reject stale emissions.
        // No credentials, HTTP request or current editable text belong in this local read function.
        // Finish check: a previously synced query reopens offline in the same order with the same next offset.
        throw UnsupportedOperationException("Complete SEARCH-2c: observe cached search")
    }

    suspend fun storePage(userId: String, query: OpportunitySearchQuery, page: OpportunitySearchPage, replace: Boolean) {
        // TODO [SEARCH-2C] Write shared cards and this query's membership/progress in ONE transaction.
        // 1. Derive the normalized query key; validate that the page applies to that submitted query.
        // 2. Map/upsert shared Opportunity rows with userId first, then upsert query metadata before child
        //    memberships. Every foreign key uses the same account; never share a user's isWatched with another.
        // 3. For replace=true remove only this query's old memberships/progress and write the new first page.
        //    For append preserve previous positions, append previously unseen IDs in response order, and
        //    update existing cards without inserting duplicate memberships for IDs already present.
        // 4. Save total/nextOffset/hasSynced in the same commit. Use the server's offset even when duplicates
        //    reduced the number of newly visible cards. A failed transaction must not advance page progress.
        // 5. Preserve all other queries, Discovery memberships and Browse memberships. The repository must
        //    hold OpportunitySyncCoordinator over fetch + this transaction to serialize watch changes.
        // Finish check: save A then B, refresh A and verify B/Discovery remain; fail an append mid-write and
        // confirm its old results/progress survive. NEXT: SEARCH-3 real fetching and repository orchestration.
        throw UnsupportedOperationException("Complete SEARCH-2c: persist search page atomically")
    }
}
