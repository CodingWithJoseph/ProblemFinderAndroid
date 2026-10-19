package com.example.problemfinder.data.opportunities.impl.model.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The watch endpoint expects a string ID and snake_case field, unlike the Browse card payload. */
@Serializable
data class WatchOpportunityBody(@SerialName("cluster_id") val clusterId: String)

/** Server-confirmed membership; the repository checks it matches the requested state. */
@Serializable
data class OpportunityWatchDto(@SerialName("is_watched") val isWatched: Boolean)
