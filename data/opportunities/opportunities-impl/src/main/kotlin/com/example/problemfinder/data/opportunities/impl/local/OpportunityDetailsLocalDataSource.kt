package com.example.problemfinder.data.opportunities.impl.local

import androidx.room3.withReadTransaction
import androidx.room3.withWriteTransaction
import com.example.problemfinder.core.model.details.OpportunityDetailsPage
import com.example.problemfinder.core.model.details.OpportunityDetailsSnapshot
import com.example.problemfinder.core.model.details.OpportunityEvidence
import com.example.problemfinder.data.opportunities.impl.db.OpportunitiesDatabase
import com.example.problemfinder.data.opportunities.impl.db.dao.OpportunityDetailsDao
import com.example.problemfinder.data.opportunities.impl.model.EvidenceSection
import com.example.problemfinder.data.opportunities.impl.model.entity.OpportunityDetailsCacheRow
import com.example.problemfinder.data.opportunities.impl.util.toEntity
import com.example.problemfinder.data.opportunities.impl.util.toMembershipEntities
import com.example.problemfinder.data.opportunities.impl.util.toModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Owns local Details snapshots and atomic page saves. Authentication and HTTP remain in the repository.
 *
 * Your original write rules remain: refresh replaces the cache; append preserves ordinary placements
 * while replacing the complete TOP/source/date sets. The refactor separates reading a snapshot from
 * deciding which memberships to save, and makes all snapshot reads see one database state.
 */
class OpportunityDetailsLocalDataSource @Inject constructor(
    private val database: OpportunitiesDatabase,
    private val detailsDao: OpportunityDetailsDao,
) {
    fun observeDetails(userId: String, clusterId: Int): Flow<OpportunityDetailsSnapshot> =
        // REFACTOR 1: the joined query alone did not observe the two count tables. This flow watches
        // every table used by the snapshot. Its initial notification also triggers the first cache read.
        // Notifications say "read again"; they do not contain the snapshot's data themselves.
        database.invalidationTracker.createFlow(
            "opportunity_details",
            "opportunities",
            "opportunity_evidence_membership",
            "opportunity_evidence",
            "evidence_source_count",
            "evidence_date_count",
        )
            .map { readSnapshot(userId, clusterId) }
            // Room invalidates whole tables, including writes for other accounts/clusters. Suppress
            // an emission when re-reading those tables leaves this particular snapshot unchanged.
            .distinctUntilChanged()

    private suspend fun readSnapshot(userId: String, clusterId: Int): OpportunityDetailsSnapshot =
        // REFACTOR 2: previously, joined rows arrived first and counts were queried afterward, so a
        // write between reads could mix old evidence with new counts. Read all three parts within
        // one read transaction. Collecting a Flow inside a transaction would not provide this shape.
        database.withReadTransaction {
            val rows = detailsDao.getDetailsRows(userId, clusterId)
            val firstRow = rows.firstOrNull()
                ?: return@withReadTransaction OpportunityDetailsSnapshot(userId = userId, cacheReady = true)
            val parent = firstRow.details
            val sources = detailsDao.getSourceCount(userId, clusterId).map { it.toModel() }
            val dates = detailsDao.getDateCount(userId, clusterId).map { it.toModel() }

            OpportunityDetailsSnapshot(
                userId = userId,
                details = parent.toModel(sources = sources, dates = dates),
                cachedOpportunity = firstRow.opportunity?.toModel(),
                topEvidence = rows.evidenceFor(EvidenceSection.TOP),
                posts = rows.evidenceFor(EvidenceSection.POSTS),
                lastLoadedPage = parent.lastLoadedPage,
                pageSize = parent.pageSize,
                totalPosts = parent.postCount,
                // An actual read, even an empty one, finishes cache loading. Successful network sync
                // is a separate persisted fact; an empty evidence list does not determine hasSynced.
                cacheReady = true,
                hasSynced = parent.hasSynced,
            )
        }

    suspend fun storePage(userId: String, page: OpportunityDetailsPage, replace: Boolean) {
        val clusterId = page.details.id
        val details = page.details.toEntity(
            userId = userId,
            hasSynced = true,
            pageSize = page.pageSize,
            lastLoadedPage = page.page,
        )
        val incomingMemberships = page.toMembershipEntities(userId)
        // REFACTOR 3: deduplicate shared post content before mapping, so a post appearing in both
        // TOP and POSTS is converted once. Its two section memberships are still kept separately.
        val evidence = (page.topEvidence + page.posts)
            .distinctBy { it.id }
            .map { it.toEntity(userId, clusterId) }
        val sources = page.details.sourceBreakdown.map { it.toEntity(userId, clusterId) }
        val dates = page.details.postVolumeByDate.map { it.toEntity(userId, clusterId) }

        database.withWriteTransaction {
            // REFACTOR 4: each branch returns the memberships it will save. Refresh does not need
            // saved progress or post-ID queries; append must read and validate them in this transaction.
            val membershipsToSave = if (replace) {
                require(page.page == 1) { "Replacement must start at page 1" }
                detailsDao.deleteOpportunityDetails(userId, clusterId)
                // Cascading deletes already removed every old child, including ordinary placements.
                incomingMemberships
            } else {
                val saved = detailsDao.getPaginationMetadata(userId, clusterId)
                require(
                    saved != null && saved.hasSynced && saved.pageSize == page.pageSize &&
                        page.page == saved.lastLoadedPage + 1
                ) { "Response does not continue the cached pages" }

                // REFACTOR 5: a Set avoids scanning every saved ID for each incoming membership.
                // Filter placements only: existing post content must still receive incoming updates.
                val savedPostIds = detailsDao.getStandardPostIds(userId, clusterId).toSet()
                detailsDao.clearMembershipsForSection(userId, clusterId, EvidenceSection.TOP)
                detailsDao.clearEvidenceSourceCount(userId, clusterId)
                detailsDao.clearEvidenceDateCount(userId, clusterId)
                incomingMemberships.filter {
                    it.section == EvidenceSection.TOP || it.postId !in savedPostIds
                }
            }

            // The parent must exist before its foreign-key children. Although its progress is written
            // first, no change becomes visible until the whole transaction succeeds; failure rolls back
            // the deletions, content, placements, counts, and progress together.
            detailsDao.upsertOpportunityDetails(details)
            detailsDao.upsertOpportunityEvidence(evidence)
            detailsDao.upsertOpportunityEvidenceMembership(membershipsToSave)
            detailsDao.upsertEvidenceSourceCount(sources)
            detailsDao.upsertEvidenceDateCount(dates)
        }
    }

    private fun List<OpportunityDetailsCacheRow>.evidenceFor(
        section: EvidenceSection,
    ): List<OpportunityEvidence> =
        // REFACTOR 6: keep SQL's page/position order, work within one section, and deduplicate before
        // mapping. The sequence avoids intermediate lists; TOP and POSTS may still share the same post.
        asSequence()
            .filter { it.membership?.section == section }
            .mapNotNull { it.evidence }
            .distinctBy { it.postId }
            .map { it.toModel() }
            .toList()
}
