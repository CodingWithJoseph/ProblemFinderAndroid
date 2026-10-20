package com.example.problemfinder.data.opportunities.impl.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class OpportunityDetailsDto(
    val id: Int,
    val name: String? = null,
    val summary: String? = null,
    val problemStatement: String? = null,
    val postCount: Int? = null,
    val firstSeen: String? = null,
    val lastSeenDate: String? = null,
    val authorCount: Int = 0,
    val communityCount: Int = 0,
    val sourceBreakdown: List<EvidenceSourceCountDto> = emptyList(),
    val postVolumeByDate: List<EvidenceDateCountDto> = emptyList(),
    val topEvidence: List<OpportunityEvidenceDto> = emptyList(),
    val posts: List<OpportunityEvidenceDto> = emptyList(),
    val page: Int,
    val pageSize: Int,
    val totalPosts: Int,
)

@Serializable data class EvidenceSourceCountDto(val platform: String, val count: Int)
@Serializable data class EvidenceDateCountDto(val date: String, val count: Int)

@Serializable
data class OpportunityEvidenceDto(
    val id: Int,
    val title: String,
    val body: String? = null,
    val url: String? = null,
    val author: String? = null,
    val community: String? = null,
    val source: String? = null,
    val score: Int? = null,
    val numComments: Int? = null,
    val postedAt: String? = null,
    val problemStatement: String? = null,
)
