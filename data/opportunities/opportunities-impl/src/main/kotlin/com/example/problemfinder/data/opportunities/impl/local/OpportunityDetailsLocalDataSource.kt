package com.example.problemfinder.data.opportunities.impl.local

import androidx.room3.withWriteTransaction
import com.example.problemfinder.core.model.details.OpportunityDetailsPage
import com.example.problemfinder.core.model.details.OpportunityDetailsSnapshot
import com.example.problemfinder.core.model.details.OpportunityEvidence
import com.example.problemfinder.data.opportunities.impl.db.OpportunitiesDatabase
import com.example.problemfinder.data.opportunities.impl.db.dao.OpportunityDetailsDao
import com.example.problemfinder.data.opportunities.impl.model.EvidenceSection
import com.example.problemfinder.data.opportunities.impl.model.entity.EvidenceDateCountEntity
import com.example.problemfinder.data.opportunities.impl.model.entity.EvidenceSourceCountEntity
import com.example.problemfinder.data.opportunities.impl.model.entity.OpportunityDetailsCacheRow
import com.example.problemfinder.data.opportunities.impl.util.toEntity
import com.example.problemfinder.data.opportunities.impl.util.toMembershipEntities
import com.example.problemfinder.data.opportunities.impl.util.toModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class OpportunityDetailsLocalDataSource @Inject constructor(
    private val database: OpportunitiesDatabase,
    private val discoverDetailsDao: OpportunityDetailsDao
) {
    fun observeDetails(userId: String, clusterId: Int): Flow<OpportunityDetailsSnapshot> =
        discoverDetailsDao.observeDetails(userId, clusterId).map { rows ->

            val firstRow = rows.firstOrNull() ?: return@map OpportunityDetailsSnapshot(
                userId = userId,
                cacheReady = true
            )

            val topEvidence = rows.evidenceFor(EvidenceSection.TOP)
            val postEvidence = rows.evidenceFor(EvidenceSection.POSTS)

            val sourceCounts = getSourceCount(userId, clusterId).map {
                it.toModel()
            }
            val dateCounts = getDateCount(userId, clusterId).map {
                it.toModel()
            }

            val parent = firstRow.details

            OpportunityDetailsSnapshot(
                userId = userId,
                details = parent.toModel(
                    sources = sourceCounts,
                    dates = dateCounts
                ),
                cachedOpportunity = firstRow.opportunity?.toModel(),
                topEvidence = topEvidence,
                posts = postEvidence,
                lastLoadedPage = parent.lastLoadedPage,
                pageSize = parent.pageSize,
                totalPosts = parent.postCount,
                hasSynced = parent.hasSynced,
                cacheReady = true
            )
        }

    suspend fun storePage(userId: String, page: OpportunityDetailsPage, replace: Boolean) {
        /**
         * Three pieces still need attention:
         * 1. Validate pagination before writing.
         *    Replacement should accept page 1. Appending should require an existing synced parent, the same pageSize, and
         *    incomingPage == lastLoadedPage + 1. Read and check that stored progress inside the transaction. Otherwise,
         *    saving page 3 into an empty cache would incorrectly claim that pages 1–3 were loaded.
         *
         * 2. Replace the complete TOP and statistics sets during append.
         *    Every response contains the current complete highlighted list and aggregate counts. Upsert updates
         *    supplied rows but leaves old rows behind. For example, if highlighted posts change from [A, B] to [A, C],
         *    upserting leaves B highlighted too. Clear this account/cluster’s TOP memberships and source/date count
         *    rows before inserting their replacements. Keep earlier POSTS memberships.
         *
         * 3. Keep an ordinary post’s original placement when pages overlap.
         *    Your distinctBy handles evidence content within the incoming response. However, if page 2 repeats a post
         *    already stored on page 1, upserting its POSTS membership moves its page/position. Filter already-stored
         *    ordinary memberships from incoming placements while still updating their evidence content.
         *
         * The existing data-module unit tests pass, but they don’t yet test these Details storage scenarios.
         * The next useful slice is adding DAO reads for saved progress and existing POSTS memberships.
         */
        val parent = page.details

        val details = parent.toEntity(
            userId = userId,
            hasSynced = true,
            pageSize = page.pageSize,
            lastLoadedPage = page.page
        )

        val membership = page.toMembershipEntities(userId)

        val topEvidence = page.topEvidence.map { it.toEntity(userId, parent.id) }
        val postEvidence = page.posts.map { it.toEntity(userId, parent.id) }
        val evidence = (topEvidence + postEvidence).distinctBy { it.postId }


        val sourceCounts = parent.sourceBreakdown.map { it.toEntity(userId, parent.id) }
        val dateCounts = parent.postVolumeByDate.map { it.toEntity(userId, parent.id) }

        database.withWriteTransaction {
            if (replace) {
                discoverDetailsDao.deleteOpportunityDetails(userId, details.clusterId)
            }

            //Save details and evidence
            discoverDetailsDao.upsertOpportunityDetails(details)
            discoverDetailsDao.upsertOpportunityEvidence(evidence)

            //Save evidence membership
            discoverDetailsDao.upsertOpportunityEvidenceMembership(membership)

            //Save counts
            discoverDetailsDao.upsertEvidenceSourceCount(sourceCounts)
            discoverDetailsDao.upsertEvidenceDateCount(dateCounts)
        }
    }

    private suspend fun getSourceCount(
        userId: String,
        clusterId: Int
    ): List<EvidenceSourceCountEntity> =
        discoverDetailsDao.getSourceCount(userId, clusterId)

    private suspend fun getDateCount(
        userId: String,
        clusterId: Int
    ): List<EvidenceDateCountEntity> =
        discoverDetailsDao.getDateCount(userId, clusterId)

    private fun List<OpportunityDetailsCacheRow>.evidenceFor(section: EvidenceSection): List<OpportunityEvidence> =
        filter { it.membership?.section == section }
            .mapNotNull { it.evidence?.toModel() }
            .distinctBy { it.id }
}
