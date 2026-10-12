package com.example.problemfinder.data.browse.impl.model.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(
    tableName = "browse_clusters",
    primaryKeys = ["userId", "clusterId"]
)
data class BrowseClusterEntity(
    val userId: String,
    val clusterId: Int,
    val name: String?,
    val summary: String?,
    val problemStatement: String?,
    val postCount: Int,
    val isWatched: Boolean,
    val dateIso: String?
)
