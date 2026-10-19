package com.example.problemfinder.data.opportunities.impl.model.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WatchOpportunityBody(@SerialName("cluster_id") val clusterId: String)

@Serializable
data class OpportunityWatchDto(@SerialName("is_watched") val isWatched: Boolean)
