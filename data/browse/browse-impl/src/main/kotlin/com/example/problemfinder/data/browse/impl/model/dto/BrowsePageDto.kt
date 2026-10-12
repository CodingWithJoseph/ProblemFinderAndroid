package com.example.problemfinder.data.browse.impl.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class BrowsePageDto(
    val clusters: List<DiscoveryClusterDto>,
    val nextCursor: String?
)
