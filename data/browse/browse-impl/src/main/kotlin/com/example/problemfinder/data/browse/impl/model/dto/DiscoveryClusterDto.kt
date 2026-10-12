package com.example.problemfinder.data.browse.impl.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class DiscoveryClusterDto(
    val id: Int,
    val name: String?,
    val summary: String?,
    val problemStatement: String?,
    val postCount: Int,
    val isWatched: Boolean,
    val date: String?
)
