package com.example.problemfinder.data.opportunities.impl.model.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * One card per account/cluster pair, shared by both features. Account is part of the key because
 * isWatched belongs to the user even when the underlying cluster ID is the same.
 */
@Entity(
    tableName = "opportunities",
    primaryKeys = ["userId", "clusterId"]
)
data class OpportunityEntity(
    val userId: String,
    val clusterId: Int,
    val name: String,
    val summary: String,
    val problemStatement: String,
    val postCount: Int,
    val category: String,
    val isWatched: Boolean,
    val dateIso: String?
)
