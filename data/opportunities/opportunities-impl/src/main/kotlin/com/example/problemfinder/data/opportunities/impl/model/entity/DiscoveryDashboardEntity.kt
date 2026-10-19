package com.example.problemfinder.data.opportunities.impl.model.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "discovery_dashboard")
data class DiscoveryDashboardEntity(
    @PrimaryKey val userId: String,
    val featuredClusterId: Int?,
    val hasSynced: Boolean
)
