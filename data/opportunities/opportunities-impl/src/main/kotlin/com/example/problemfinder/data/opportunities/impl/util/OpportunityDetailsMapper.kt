package com.example.problemfinder.data.opportunities.impl.util

import com.example.problemfinder.core.common.util.toDateInstant
import com.example.problemfinder.core.model.details.EvidenceDateCount
import com.example.problemfinder.core.model.details.EvidenceSourceCount
import com.example.problemfinder.core.model.details.OpportunityDetails
import com.example.problemfinder.core.model.details.OpportunityDetailsPage
import com.example.problemfinder.core.model.details.OpportunityEvidence
import com.example.problemfinder.data.opportunities.impl.model.dto.EvidenceDateCountDto
import com.example.problemfinder.data.opportunities.impl.model.dto.EvidenceSourceCountDto
import com.example.problemfinder.data.opportunities.impl.model.dto.OpportunityDetailsDto
import com.example.problemfinder.data.opportunities.impl.model.dto.OpportunityEvidenceDto
import com.example.problemfinder.data.opportunities.impl.model.entity.EvidenceDateCountEntity
import com.example.problemfinder.data.opportunities.impl.model.EvidenceSection
import com.example.problemfinder.data.opportunities.impl.model.entity.EvidenceSourceCountEntity
import com.example.problemfinder.data.opportunities.impl.model.entity.OpportunityDetailsEntity
import com.example.problemfinder.data.opportunities.impl.model.entity.OpportunityEvidenceEntity
import com.example.problemfinder.data.opportunities.impl.model.entity.OpportunityEvidenceMembershipEntity
import kotlin.String

internal fun OpportunityDetailsDto.toDetailsPage(): OpportunityDetailsPage = OpportunityDetailsPage(
    details = this.toModel(),
    topEvidence = this.topEvidence.map { it.toModel() },
    posts = this.posts.map { it.toModel() },
    page = this.page,
    pageSize = this.pageSize,
    totalPosts = this.totalPosts
)

internal fun OpportunityDetailsDto.toModel(): OpportunityDetails = OpportunityDetails(
    id = this.id,
    name = requireNotNull(this.name).trim(),
    summary = requireNotNull(this.summary).trim(),
    problemStatement = requireNotNull(this.problemStatement).trim(),
    postCount = requireNotNull(this.postCount),
    firstSeen = this.firstSeen?.trim()?.toDateInstant(),
    lastSeen = this.lastSeenDate?.trim()?.toDateInstant(),
    authorCount = this.authorCount,
    communityCount = this.communityCount,
    sourceBreakdown = this.sourceBreakdown.map { it.toModel() },
    postVolumeByDate = this.postVolumeByDate.map { it.toModel() }
)

internal fun OpportunityEvidenceDto.toModel(): OpportunityEvidence = OpportunityEvidence(
    id = this.id,
    title = this.title,
    body = this.body,
    url = this.url,
    author = this.author,
    community = this.community,
    source = this.source,
    score = this.score,
    commentCount = this.numComments,
    postedAt = this.postedAt?.toDateInstant(),
    problemStatement = this.problemStatement
)

internal fun EvidenceSourceCountDto.toModel(): EvidenceSourceCount = EvidenceSourceCount(
    source = this.platform,
    count = this.count
)


internal fun EvidenceDateCountDto.toModel(): EvidenceDateCount = EvidenceDateCount(
    count = this.count,
    date = this.date
)


internal fun OpportunityDetails.toEntity(
    userId: String,
    hasSynced: Boolean,
    pageSize: Int,
    lastLoadedPage: Int
): OpportunityDetailsEntity =
    OpportunityDetailsEntity(
        userId = userId,
        clusterId = this.id,
        name = this.name,
        summary = this.summary,
        problemStatement = this.problemStatement,
        postCount = this.postCount,
        firstSeen = this.firstSeen,
        lastSeen = this.lastSeen,
        authorCount = this.authorCount,
        communityCount = this.communityCount,
        hasSynced = hasSynced,
        pageSize = pageSize,
        lastLoadedPage = lastLoadedPage
    )

internal fun OpportunityDetailsEntity.toModel(
    sources: List<EvidenceSourceCount>, dates: List<EvidenceDateCount>
): OpportunityDetails =
    OpportunityDetails(
        id = this.clusterId,
        name = this.name,
        summary = this.summary,
        problemStatement = this.problemStatement,
        postCount = this.postCount,
        firstSeen = this.firstSeen,
        lastSeen = this.lastSeen,
        authorCount = this.authorCount,
        communityCount = this.communityCount,
        sourceBreakdown = sources,
        postVolumeByDate = dates
    )

internal fun OpportunityEvidence.toEntity(
    userId: String,
    clusterId: Int,
): OpportunityEvidenceEntity = OpportunityEvidenceEntity(
    userId = userId,
    clusterId = clusterId,
    postId = this.id,
    title = this.title,
    body = this.body,
    url = this.url,
    author = this.author,
    community = this.community,
    source = this.source,
    score = this.score,
    commentCount = this.commentCount,
    postedAt = this.postedAt,
    problemStatement = this.problemStatement
)

internal fun OpportunityEvidenceEntity.toModel(): OpportunityEvidence =
    OpportunityEvidence(
        id = this.postId,
        title = this.title,
        body = this.body,
        url = this.url,
        author = this.author,
        community = this.community,
        source = this.source,
        score = this.score,
        commentCount = this.commentCount,
        postedAt = this.postedAt,
        problemStatement = this.problemStatement
    )

internal fun EvidenceDateCount.toEntity(userId: String, clusterId: Int): EvidenceDateCountEntity =
    EvidenceDateCountEntity(
        userId = userId,
        clusterId = clusterId,
        date = this.date,
        count = this.count
    )


internal fun EvidenceDateCountEntity.toModel(): EvidenceDateCount = EvidenceDateCount(
    count = this.count,
    date = this.date
)


internal fun EvidenceSourceCount.toEntity(
    userId: String,
    clusterId: Int
): EvidenceSourceCountEntity = EvidenceSourceCountEntity(
    userId = userId,
    clusterId = clusterId,
    source = this.source,
    count = this.count
)


internal fun EvidenceSourceCountEntity.toModel(): EvidenceSourceCount = EvidenceSourceCount(
    count = this.count,
    source = this.source
)


internal fun OpportunityDetailsPage.toMembershipEntities(userId: String): List<OpportunityEvidenceMembershipEntity> {
    val topPost = topEvidence.mapIndexed { index, evidence ->
        OpportunityEvidenceMembershipEntity(
            userId = userId,
            clusterId = details.id,
            postId = evidence.id,
            section = EvidenceSection.TOP,
            page = 0,
            position = index
        )
    }

    val standardPost = posts.mapIndexed { index, evidence ->
        OpportunityEvidenceMembershipEntity(
            userId = userId,
            clusterId = details.id,
            postId = evidence.id,
            section = EvidenceSection.POSTS,
            page = page,
            position = index
        )
    }
    return topPost + standardPost
}


