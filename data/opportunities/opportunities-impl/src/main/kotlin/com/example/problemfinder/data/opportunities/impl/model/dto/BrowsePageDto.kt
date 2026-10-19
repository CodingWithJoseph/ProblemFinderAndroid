package com.example.problemfinder.data.opportunities.impl.model.dto

import kotlinx.serialization.Serializable

/** Cursor page in server order. nextCursor is opaque to the client; null marks the final page. */
@Serializable
data class BrowsePageDto(
    val clusters: List<OpportunityDto>,
    val nextCursor: String?
)
