package com.example.problemfinder.data.opportunities.impl.util

import com.example.problemfinder.core.model.browse.BrowsePage
import com.example.problemfinder.core.model.opportunity.Opportunity
import com.example.problemfinder.core.model.discovery.DiscoveryCollection
import com.example.problemfinder.core.model.discovery.DiscoveryDashboard
import com.example.problemfinder.core.model.discovery.DiscoverySnapshot
import com.example.problemfinder.core.model.opportunity.toOpportunityCategory
import com.example.problemfinder.data.opportunities.impl.model.dto.BrowsePageDto
import com.example.problemfinder.data.opportunities.impl.model.dto.OpportunityDto
import com.example.problemfinder.data.opportunities.impl.model.dto.DiscoveryCollectionDto
import com.example.problemfinder.data.opportunities.impl.model.dto.DiscoveryDashboardDto
import com.example.problemfinder.data.opportunities.impl.model.entity.OpportunityEntity
import com.example.problemfinder.data.opportunities.impl.model.entity.DiscoveryCacheRow
import kotlin.time.Instant


fun OpportunityDto.toModel(): Opportunity = Opportunity(
    id = id,
    name = name.trim(),
    summary = summary.trim(),
    problemStatement = problemStatement.trim(),
    postCount = postCount,
    category = category.toOpportunityCategory(),
    isWatched = isWatched,
    date = date?.let { Instant.parse(it) },
)

fun BrowsePageDto.toModel(): BrowsePage = BrowsePage(
    clusters = clusters.map { it.toModel() },
    nextCursor = nextCursor
)

fun Opportunity.toEntity(userId: String): OpportunityEntity = OpportunityEntity(
    userId = userId,
    clusterId = id,
    name = name.trim(),
    summary = summary.trim(),
    problemStatement = problemStatement.trim(),
    postCount = postCount,
    category = category.toString().lowercase(),
    isWatched = isWatched,
    dateIso = date?.toString()
)

fun OpportunityEntity.toModel(): Opportunity =
    Opportunity(
        id = clusterId,
        name = name.trim(),
        summary = summary.trim(),
        problemStatement = problemStatement.trim(),
        postCount = postCount,
        category = category.toOpportunityCategory(),
        isWatched = isWatched,
        date = dateIso?.let { Instant.parse(it) }
    )


fun DiscoveryCollectionDto.toModel(): DiscoveryCollection = DiscoveryCollection(
    category = this.category.toOpportunityCategory(),
    clusters = this.clusters.map {
        it.toModel()
    }
)

fun DiscoveryDashboardDto.toModel(): DiscoveryDashboard = DiscoveryDashboard(
    featuredCluster = this.featured?.toModel(),
    collections = collections.map {
        it.toModel()
    }
)

fun List<DiscoveryCacheRow>.toDiscoverySnapshot(userId: String): DiscoverySnapshot {
    val firstRow = firstOrNull()
    val collections = groupBy {
        it.collection?.category
    }.mapNotNull { (category, rows) ->
        if (category == null) return@mapNotNull null

        DiscoveryCollection(
            category = category.toOpportunityCategory(),
            clusters = rows.mapNotNull { row ->
                row.cluster?.toModel()
            }
        )
    }

    return DiscoverySnapshot(
        userId = userId,
        dashboard = DiscoveryDashboard(
            featuredCluster = firstRow?.featured?.toModel(),
            collections = collections
        ),
        hasSynced = firstRow?.dashboard?.hasSynced ?: false,
        cacheReady = true
    )
}
