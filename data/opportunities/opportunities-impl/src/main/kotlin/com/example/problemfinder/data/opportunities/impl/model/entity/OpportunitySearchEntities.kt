package com.example.problemfinder.data.opportunities.impl.model.entity

/**
 * TODO [SEARCH-2A] Finish these key blueprints after request/page mapping and cacheKey tests pass.
 * 1. Query metadata: keep userId+queryKey as the composite primary key. Add hasSynced, total and
 *    nextOffset, plus enough normalized query metadata to reconstruct/verify the submitted query.
 * 2. Membership: userId+queryKey identifies the parent query; clusterId identifies a shared
 *    OpportunityEntity; position preserves backend order. Add composite FKs containing userId
 *    to BOTH parents, with indexes for the join columns. Enforce unique membership per result ID.
 * 3. Use the EXISTING OpportunityEntity for card fields/watch state. Search does not need a second
 *    opportunity table. Its new tables describe which cards belong to each query and in what order.
 * 4. Add @Entity/table names, keys and actual stored fields; register them/DAO on OpportunitiesDatabase.
 *    Advance the current schema version and add/test a migration retaining Browse/Discovery data.
 *    Coordinate with Details if it already changed the version; do not apply two competing 1->2 plans.
 * 5. Add query/membership write conversions and joined-row -> snapshot conversion near the search
 *    mapper/local source. Reuse existing Opportunity model <-> entity mapping for shared card rows.
 * Finish check: account A/query A and account B/query A remain separate; another filter/sort has
 * different membership/progress; one Add/Remove update can change the shared card in all views.
 * NEXT: SEARCH-2B DAO read/write queries, then SEARCH-2C local transactions.
 */
internal data class OpportunitySearchQueryEntity(val userId: String, val queryKey: String)
internal data class OpportunitySearchQueryItemEntity(
    val userId: String,
    val queryKey: String,
    val clusterId: Int,
    val position: Int,
)
