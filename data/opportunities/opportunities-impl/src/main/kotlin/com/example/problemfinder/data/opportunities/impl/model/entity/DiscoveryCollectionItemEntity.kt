package com.example.problemfinder.data.opportunities.impl.model.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index

@Entity(
    tableName = "discovery_collection_items",
    primaryKeys = ["userId", "category", "clusterId"],
    foreignKeys = [
        ForeignKey(
            entity = DiscoveryCollectionEntity::class,
            parentColumns = ["userId", "category"],
            childColumns = ["userId", "category"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = OpportunityEntity::class,
            parentColumns = ["userId", "clusterId"],
            childColumns = ["userId", "clusterId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(
            value = ["userId", "category", "position"],
            unique = true
        ),
        Index(
            value = ["userId", "clusterId"]
        )
    ]
)
data class DiscoveryCollectionItemEntity(
    val userId: String,
    val category: String,
    val clusterId: Int,
    val position: Int
)
