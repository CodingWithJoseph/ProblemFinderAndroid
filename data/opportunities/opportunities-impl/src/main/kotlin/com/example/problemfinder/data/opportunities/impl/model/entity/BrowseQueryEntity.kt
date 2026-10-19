package com.example.problemfinder.data.opportunities.impl.model.entity

import androidx.room3.Entity

/**
 * Metadata for one account/filter/sort query. hasSynced distinguishes an unfetched query from a
 * successful empty result; a null nextCursor means no more pages after a successful sync.
 */
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
