package com.example.problemfinder.data.opportunities.impl.model.entity

import androidx.room3.Entity

@Entity(
    tableName = "browse_queries",
    primaryKeys = ["userId", "category", "sort"]
)
data class BrowseQueryEntity(
    val userId: String,
    val category: String,
    val sort: String,
    val nextCursor: String?,
    val hasSynced: Boolean
)
