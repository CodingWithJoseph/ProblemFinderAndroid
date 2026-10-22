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


/** Normalizes text and parses time; Opportunity's constructor rejects invalid card content. */
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

/** Preserves server ordering and the opaque cursor while validating every card. */
fun BrowsePageDto.toModel(): BrowsePage = BrowsePage(
    clusters = clusters.map { it.toModel() },
    nextCursor = nextCursor
)

/** Adds cache ownership and stores the full timestamp as ISO text without truncating precision. */
fun Opportunity.toModel(userId: String): OpportunityEntity = OpportunityEntity(
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

/** Reconstructs and validates a cached card using the same category/time rules as network content. */
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


/** Keeps collection membership category independent from the categories of its cards. */
fun DiscoveryCollectionDto.toModel(): DiscoveryCollection = DiscoveryCollection(
    category = this.category.toOpportunityCategory(),
    clusters = this.clusters.map {
        it.toModel()
    }
)

/** Maps a whole dashboard before local persistence; any invalid nested card fails the conversion. */
fun DiscoveryDashboardDto.toModel(): DiscoveryDashboard = DiscoveryDashboard(
    featuredCluster = this.featured?.toModel(),
    collections = collections.map {
        it.toModel()
    }
)

/**
 * Reassembles the flattened Room join, preserving the DAO's collection and card order. An empty
 * row list completes the cache read (cacheReady), but only stored metadata can claim a prior sync.
 */
fun List<DiscoveryCacheRow>.toDiscoverySnapshot(userId: String): DiscoverySnapshot {
    val firstRow = firstOrNull()
    val collections = groupBy {
        it.collection?.category
    }.mapNotNull { (category, rows) ->
        // No header means no collection; a header with null card data is a real empty collection.
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
