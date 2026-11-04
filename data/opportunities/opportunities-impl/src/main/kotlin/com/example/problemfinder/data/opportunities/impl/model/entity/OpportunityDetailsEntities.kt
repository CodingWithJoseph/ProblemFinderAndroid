package com.example.problemfinder.data.opportunities.impl.model.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import com.example.problemfinder.data.opportunities.impl.model.EvidenceSection
import kotlin.time.Instant

@Entity(
    tableName = "opportunity_details",
    primaryKeys = ["userId", "clusterId"]
)
data class OpportunityDetailsEntity(
    val userId: String,
    val clusterId: Int,
    val name: String,
    val summary: String,
    val problemStatement: String,
    val postCount: Int,
    val firstSeen: Instant?,
    val lastSeen: Instant?,
    val authorCount: Int,
    val communityCount: Int,
    val hasSynced: Boolean,
    val pageSize: Int,
    val lastLoadedPage: Int
)

@Entity(
    tableName = "opportunity_evidence",
    primaryKeys = ["userId", "clusterId", "postId"],
    foreignKeys = [
        ForeignKey(
            entity = OpportunityDetailsEntity::class,
            parentColumns = ["userId", "clusterId"],
            childColumns = ["userId", "clusterId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class OpportunityEvidenceEntity(
    val userId: String,
    val clusterId: Int,
    val postId: String,
    val title: String,
    val body: String?,
    val url: String?,
    val author: String?,
    val community: String?,
    val source: String?,
    val score: Int?,
    val commentCount: Int?,
    val postedAt: Instant?,
    val problemStatement: String?,
)

@Entity(
    tableName = "opportunity_evidence_membership",
    primaryKeys = ["userId", "clusterId", "postId", "section"],
    foreignKeys = [
        ForeignKey(
            entity = OpportunityDetailsEntity::class,
            parentColumns = ["userId", "clusterId"],
            childColumns = ["userId", "clusterId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = OpportunityEvidenceEntity::class,
            parentColumns = ["userId", "clusterId", "postId"],
            childColumns = ["userId", "clusterId", "postId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["userId", "clusterId", "section", "page", "position"])
    ]
)
data class OpportunityEvidenceMembershipEntity(
    val userId: String,
    val clusterId: Int,
    val postId: String,
    val section: EvidenceSection,
    val page: Int,
    val position: Int,
)

@Entity(
    tableName = "evidence_source_count",
    primaryKeys = ["userId", "clusterId", "source"],
    foreignKeys = [
        ForeignKey(
            entity = OpportunityDetailsEntity::class,
            parentColumns = ["userId", "clusterId"],
            childColumns = ["userId", "clusterId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class EvidenceSourceCountEntity(
    val userId: String,
    val clusterId: Int,
    val source: String,
    val count: Int
)

@Entity(
    tableName = "evidence_date_count",
    primaryKeys = ["userId", "clusterId", "date"],
    foreignKeys = [
        ForeignKey(
            entity = OpportunityDetailsEntity::class,
            parentColumns = ["userId", "clusterId"],
            childColumns = ["userId", "clusterId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class EvidenceDateCountEntity(
    val userId: String,
    val clusterId: Int,
    val date: String,
    val count: Int
)

data class PaginationMetadata (
    val hasSynced: Boolean,
    val pageSize: Int,
    val lastLoadedPage: Int
)
