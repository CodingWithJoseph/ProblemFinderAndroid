package com.example.problemfinder.data.opportunities.impl.model.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

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
