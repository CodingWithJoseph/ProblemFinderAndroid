package com.example.problemfinder.data.opportunities.impl.model.entity

/**
 * TODO [DETAILS-2A] Start persistence only after DTO -> model mapping passes its tests.
 * The classes below are KEY BLUEPRINTS. They are not complete entities and create no Room tables.
 *
 * Complete the schema in this order:
 * 1. OpportunityDetailsEntity: add full validated parent text/counts/timestamps. Store timestamps
 *    as ISO text with complete precision. Add hasSynced, pageSize, totalPosts and lastLoadedPage
 *    (or equivalent page metadata) so progress survives restart. Primary key: userId + clusterId.
 * 2. OpportunityEvidenceEntity: add the post's title/body/URL/author/community/source/score/comment
 *    count/problem statement/time. Key: userId + clusterId + postId; reference the parent details row.
 * 3. OpportunityEvidenceMembershipEntity: retain section, page and position to distinguish top
 *    evidence from ordinary pages. Each membership references the evidence and parent using the
 *    FULL account-scoped keys. The same evidence content can have both top and page memberships.
 * 4. Add @Entity/table names, composite primary/foreign keys and indexes matching the lookup/join
 *    columns. Choose cascade behavior consciously; deleting an account's details must not delete
 *    shared Browse/Discovery cards or another account's rows.
 * 5. Persist aggregate source/date lists too. They are display-only: explicit JSON converters are
 *    one simple choice; child tables are useful if you want SQL queries over the aggregates. Choose
 *    one representation, implement both conversion directions and round-trip-test it.
 * 6. Add model -> entity and entity -> model functions in OpportunityDetailsMapper.kt. Every entity
 *    needs account ownership; never use a global cluster ID as a substitute for userId.
 * 7. Register the completed entities/DAO in OpportunitiesDatabase, advance its CURRENT version and
 *    write/test the migration. The scaffold starts at version 1, but do not hardcode that assumption
 *    after another feature advances it. Keep all existing Browse/Discovery entities and data.
 *
 * The shared Opportunity row is optional for a cold detail link. Do not make it a mandatory parent
 * that prevents storing details. Its membership/category remain authoritative when present.
 * Finish check: a model can round-trip through cache values without losing optional fields/order;
 * two users with equal cluster/post IDs remain isolated. NEXT: DETAILS-2B DAO queries.
 */
internal data class OpportunityDetailsEntity(val userId: String, val clusterId: Int)
internal data class OpportunityEvidenceEntity(val userId: String, val clusterId: Int, val postId: Int)
internal data class OpportunityEvidenceMembershipEntity(
    val userId: String,
    val clusterId: Int,
    val postId: Int,
    val section: String,
    val page: Int,
    val position: Int,
)
