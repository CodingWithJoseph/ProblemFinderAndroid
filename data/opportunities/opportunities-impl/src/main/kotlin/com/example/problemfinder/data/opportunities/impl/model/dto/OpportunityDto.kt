package com.example.problemfinder.data.opportunities.impl.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class OpportunityDto(
    val id: Int,
    val name: String,
    val summary: String,
    val problemStatement: String,
    val postCount: Int,
    val category: String,
    val isWatched: Boolean,
    val date: String?
)
