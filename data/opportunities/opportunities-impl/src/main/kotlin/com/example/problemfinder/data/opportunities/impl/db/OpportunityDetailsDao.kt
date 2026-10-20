package com.example.problemfinder.data.opportunities.impl.db

import com.example.problemfinder.data.opportunities.impl.model.entity.OpportunityDetailsEntity
import kotlinx.coroutines.flow.Flow

/**
 * TODO [DETAILS-2B] Write SQL access after the entity fields/relationships exist.
 * This interface is not annotated/registered yet. Its single method is a starting shape, not a
 * finished join or a complete list of queries. Complete it rather than creating a competing DAO.
 *
 * Write next:
 * 1. Add @Dao. Add @Upsert methods for parent/evidence/memberships and queries to remove only the
 *    selected account+cluster's old memberships when refreshing. Deleting every account is a bug.
 * 2. Add an observation query/read model that starts from account+cluster details metadata and
 *    LEFT JOINs evidence membership/content. Keep the parent if it has no evidence memberships.
 * 3. LEFT JOIN the existing opportunities row on BOTH userId and clusterId for authoritative watch
 *    state. Absence of that row must not remove the details parent or imply isWatched=false.
 * 4. Select the fields needed to reconstruct the snapshot, including stored sync/paging metadata.
 *    A flattened join needs a read-model class, as BrowseCacheRow/DiscoveryCacheRow demonstrate.
 *    If separate flows are clearer, combine them in the local source and preserve transaction coherence.
 * 5. Order top entries by position; page entries by page then position. Give columns aliases when
 *    embedded records would otherwise share a name. All child joins retain account ownership.
 * 6. Expose the DAO from OpportunitiesDatabase and bind its instance in OpportunitiesDatabaseModule.
 *    Then inject it/database into OpportunityDetailsLocalDataSource.
 *
 * Finish check: Room returns full text plus ordered evidence, a no-membership parent still appears,
 * and OpportunityDao.setWatched invalidates this observation without a second HTTP request.
 * NEXT: DETAILS-2C local reads/transactions; keep network work out of this DAO.
 */
internal interface OpportunityDetailsDao {
    fun observeDetails(userId: String, clusterId: Int): Flow<OpportunityDetailsEntity?>
}
