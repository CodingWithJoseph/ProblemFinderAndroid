package com.example.problemfinder.data.opportunities.impl.db.dao

import com.example.problemfinder.data.opportunities.impl.model.entity.OpportunitySearchQueryEntity
import kotlinx.coroutines.flow.Flow

/**
 * TODO [SEARCH-2B] Complete the DAO after SEARCH-2A adds real annotated entities.
 * 1. Add @Dao, query-metadata/membership upserts, and a delete for only this account+query's old
 *    memberships. Shared opportunity upserts already belong to the common Opportunity DAO.
 * 2. Build a joined read model starting from query metadata, LEFT JOIN ordered query memberships,
 *    then LEFT JOIN opportunities on BOTH userId and clusterId. Preserve metadata for zero results.
 * 3. Select total/nextOffset/hasSynced even when the joined card is null. Order by stored position;
 *    sorting by ID/date locally would destroy server relevance ordering.
 * 4. Register the DAO on OpportunitiesDatabase, bind its instance in OpportunitiesDatabaseModule,
 *    then inject it/database into OpportunitySearchLocalDataSource.
 * Reference BrowseDao/BrowseCacheRow for account-scoped metadata -> membership -> card joins; adapt
 * cursor metadata to offset paging. Do not observe/filter the Discovery collection tables as Search.
 * Finish check: a saved zero-result query emits metadata, two queries retain independent order, and
 * a shared card's watch update invalidates Search without a repeat network query.
 */
internal interface OpportunitySearchDao {
    fun observeQuery(userId: String, queryKey: String): Flow<OpportunitySearchQueryEntity?>
}
