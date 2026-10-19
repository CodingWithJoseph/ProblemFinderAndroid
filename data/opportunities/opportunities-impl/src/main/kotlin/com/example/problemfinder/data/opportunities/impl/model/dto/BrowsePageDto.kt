package com.example.problemfinder.data.opportunities.impl.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class BrowsePageDto(
    val clusters: List<OpportunityDto>,
    val nextCursor: String?
)
