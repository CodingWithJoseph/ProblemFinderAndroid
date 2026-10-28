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


class OpportunityDetailsLocalDataSource @Inject constructor(
    private val database: OpportunitiesDatabase,
    private val detailsDao: OpportunityDetailsDao,
) {
    fun observeDetails(userId: String, clusterId: Int): Flow<OpportunityDetailsSnapshot> =
        database.invalidationTracker.createFlow(
            "opportunity_details",
            "opportunities",
            "opportunity_evidence_membership",
            "opportunity_evidence",
            "evidence_source_count",
            "evidence_date_count",
        ).map { readSnapshot(userId, clusterId) }
            .distinctUntilChanged()

    suspend fun getOpportunityDetails(userId: String, clusterId: Int): OpportunityDetailsSnapshot = readSnapshot(
        userId = userId,
        clusterId = clusterId
    )

    private suspend fun readSnapshot(userId: String, clusterId: Int): OpportunityDetailsSnapshot =
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
        val evidence = (page.topEvidence + page.posts)
            .distinctBy { it.id }
            .map { it.toEntity(userId, clusterId) }
        val sources = page.details.sourceBreakdown.map { it.toEntity(userId, clusterId) }
        val dates = page.details.postVolumeByDate.map { it.toEntity(userId, clusterId) }

        database.withWriteTransaction {
            val membershipsToSave = if (replace) {
                require(page.page == 1) { "Replacement must start at page 1" }
                detailsDao.deleteOpportunityDetails(userId, clusterId)
                incomingMemberships
            } else {
                val saved = detailsDao.getPaginationMetadata(userId, clusterId)
                require(
                    saved != null && saved.hasSynced && saved.pageSize == page.pageSize &&
                        page.page == saved.lastLoadedPage + 1
                ) { "Response does not continue the cached pages" }
                val savedPostIds = detailsDao.getStandardPostIds(userId, clusterId).toSet()
                detailsDao.clearMembershipsForSection(userId, clusterId, EvidenceSection.TOP)
                detailsDao.clearEvidenceSourceCount(userId, clusterId)
                detailsDao.clearEvidenceDateCount(userId, clusterId)
                incomingMemberships.filter {
                    it.section == EvidenceSection.TOP || it.postId !in savedPostIds
                }
            }
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
        asSequence()
            .filter { it.membership?.section == section }
            .mapNotNull { it.evidence }
            .distinctBy { it.postId }
            .map { it.toModel() }
            .toList()
}
