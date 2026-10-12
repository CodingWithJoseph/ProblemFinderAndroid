package com.example.problemfinder.data.browse.impl.model.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index

@Entity(
    tableName = "browse_query_items",
    primaryKeys = ["userId", "category", "sort", "clusterId"],
    foreignKeys = [
        ForeignKey(
            entity = BrowseQueryEntity::class,
            parentColumns = ["userId", "category", "sort"],
            childColumns = ["userId", "category", "sort"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = BrowseClusterEntity::class,
            parentColumns = ["userId", "clusterId"],
            childColumns = ["userId", "clusterId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(
            value = ["userId", "category", "sort", "position"],
            unique = true
        ),
        Index(value = ["userId", "clusterId"])
    ]
)
data class BrowseQueryItemEntity(
    val userId: String,
    val category: String,
    val sort: String,
    val clusterId: Int,
    val position: Int
)
