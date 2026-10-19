package com.example.problemfinder.data.opportunities.impl.model.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey

@Entity(
    tableName = "discovery_collections",
    primaryKeys = ["userId", "category"],
    foreignKeys = [
        ForeignKey(
            entity = DiscoveryDashboardEntity::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
class DiscoveryCollectionEntity(
    val userId: String,
    val category: String,
)
