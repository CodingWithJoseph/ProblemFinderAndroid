package com.example.problemfinder.data.opportunities.impl.model.entity

import androidx.room3.Embedded
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index

@Entity(tableName = "search_queries", primaryKeys = ["userId", "queryKey"])
data class OpportunitySearchQueryEntity(
    val userId: String,
    val queryKey: String,
    val total: Int,
    val nextOffset: Int?,
)

@Entity(
    tableName = "search_query_items",
    primaryKeys = ["userId", "queryKey", "clusterId"],
    foreignKeys = [
        ForeignKey(
            entity = OpportunitySearchQueryEntity::class,
            parentColumns = ["userId", "queryKey"],
            childColumns = ["userId", "queryKey"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = OpportunityEntity::class,
            parentColumns = ["userId", "clusterId"],
            childColumns = ["userId", "clusterId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["userId", "clusterId"]), Index(value = ["userId", "queryKey", "position"])],
)
data class OpportunitySearchQueryItemEntity(
    val userId: String,
    val queryKey: String,
    val clusterId: Int,
    val position: Int,
)

data class OpportunitySearchCacheRow(
    val total: Int,
    val nextOffset: Int?,
    @Embedded val card: OpportunityEntity?,
)
