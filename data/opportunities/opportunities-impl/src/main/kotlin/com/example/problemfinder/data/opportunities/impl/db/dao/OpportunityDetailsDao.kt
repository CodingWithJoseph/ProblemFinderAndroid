package com.example.problemfinder.data.opportunities.impl.db.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import com.example.problemfinder.core.model.details.EvidenceDateCount
import com.example.problemfinder.data.opportunities.impl.model.entity.EvidenceDateCountEntity
import com.example.problemfinder.data.opportunities.impl.model.entity.EvidenceSourceCountEntity
import com.example.problemfinder.data.opportunities.impl.model.entity.OpportunityDetailsCacheRow
import com.example.problemfinder.data.opportunities.impl.model.entity.OpportunityDetailsEntity
import com.example.problemfinder.data.opportunities.impl.model.entity.OpportunityEvidenceEntity
import com.example.problemfinder.data.opportunities.impl.model.entity.OpportunityEvidenceMembershipEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OpportunityDetailsDao {

    @Query(
        """
        SELECT
            d.userId AS details_userId,
            d.clusterId AS details_clusterId,
            d.name AS details_name,
            d.summary AS details_summary,
            d.problemStatement AS details_problemStatement,
            d.postCount AS details_postCount,
            d.firstSeen AS details_firstSeen,
            d.lastSeen AS details_lastSeen,
            d.authorCount AS details_authorCount,
            d.communityCount AS details_communityCount,
            d.hasSynced AS details_hasSynced,
            d.pageSize AS details_pageSize,
            d.lastLoadedPage AS details_lastLoadedPage,

            o.userId AS opportunity_userId,
            o.clusterId AS opportunity_clusterId,
            o.name AS opportunity_name,
            o.summary AS opportunity_summary,
            o.problemStatement AS opportunity_problemStatement,
            o.postCount AS opportunity_postCount,
            o.category AS opportunity_category,
            o.isWatched AS opportunity_isWatched,
            o.dateIso AS opportunity_dateIso,

            m.userId AS membership_userId,
            m.clusterId AS membership_clusterId,
            m.postId AS membership_postId,
            m.section AS membership_section,
            m.page AS membership_page,
            m.position AS membership_position,

            e.userId AS evidence_userId,
            e.clusterId AS evidence_clusterId,
            e.postId AS evidence_postId,
            e.title AS evidence_title,
            e.body AS evidence_body,
            e.url AS evidence_url,
            e.author AS evidence_author,
            e.community AS evidence_community,
            e.source AS evidence_source,
            e.score AS evidence_score,
            e.commentCount AS evidence_commentCount,
            e.postedAt AS evidence_postedAt,
            e.problemStatement AS evidence_problemStatement
        FROM opportunity_details as d
        LEFT JOIN opportunities AS o
            ON d.userId = o.userId
            AND d.clusterId = o.clusterId
        LEFT JOIN opportunity_evidence_membership AS m
            ON d.userId = m.userId
            AND d.clusterId = m.clusterId
        LEFT JOIN opportunity_evidence AS e
            ON m.userId = e.userId
            AND m.clusterId = e.clusterId
            AND m.postId = e.postId
        WHERE d.userId = :userId
        AND d.clusterId = :clusterId
        ORDER BY
            m.section ASC,
            m.page ASC,
            m.position ASC
    """
    )
    fun observeDetails(userId: String, clusterId: Int): Flow<List<OpportunityDetailsCacheRow>>

    @Query(
        """
        SELECT * FROM evidence_source_count
        WHERE userId = :userId
        AND clusterId = :clusterId
        ORDER BY source ASC
    """
    )
    suspend fun getSourceCount(userId: String, clusterId: Int): List<EvidenceSourceCountEntity>

    @Query(
        """
        SELECT * FROM evidence_date_count
        WHERE userId = :userId
        AND clusterId = :clusterId
        ORDER BY date ASC
    """
    )
    suspend fun getDateCount(userId: String, clusterId: Int): List<EvidenceDateCountEntity>

    @Upsert
    suspend fun upsertOpportunityDetails(opportunityDetails: OpportunityDetailsEntity)

    @Upsert
    suspend fun upsertOpportunityEvidence(opportunityEvidence: OpportunityEvidenceEntity)

    @Upsert
    suspend fun upsertOpportunityEvidenceMembership(opportunityEvidenceMembership: OpportunityEvidenceMembershipEntity)

    @Upsert
    suspend fun upsertEvidenceSourceCount(evidenceSourceCount: EvidenceSourceCountEntity)

    @Upsert
    suspend fun upsertEvidenceDateCount(evidenceDateCount: EvidenceDateCountEntity)

    @Query("DELETE FROM opportunity_details WHERE userId = :userId AND clusterId = :clusterId")
    suspend fun deleteOpportunityDetails(userId: String, clusterId: Int)
}
