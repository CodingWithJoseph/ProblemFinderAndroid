package com.example.problemfinder.data.browse.impl.util

import com.example.problemfinder.core.model.browse.BrowsePage
import com.example.problemfinder.core.model.discovery.DiscoveryCluster
import com.example.problemfinder.data.browse.impl.model.dto.BrowsePageDto
import com.example.problemfinder.data.browse.impl.model.dto.DiscoveryClusterDto
import com.example.problemfinder.data.browse.impl.model.entity.BrowseClusterEntity
import kotlin.String
import kotlin.time.Instant


fun DiscoveryClusterDto.toModel(): DiscoveryCluster = DiscoveryCluster(
    id = id,
    name = name,
    summary = summary,
    problemStatement = problemStatement,
    postCount = postCount,
    isWatched = isWatched,
    date = date?.let { Instant.parse(it) },
)

fun BrowsePageDto.toModel(): BrowsePage = BrowsePage(
    clusters = clusters.map { it.toModel() },
    nextCursor = nextCursor
)

fun DiscoveryCluster.toEntity(userId: String): BrowseClusterEntity = BrowseClusterEntity(
    userId = userId,
    clusterId = id,
    name = name,
    summary = summary,
    problemStatement = problemStatement,
    postCount = postCount,
    isWatched = isWatched,
    dateIso = date?.toString()
)

fun BrowseClusterEntity.toModel(): DiscoveryCluster =
    DiscoveryCluster(
        id = clusterId,
        name = name,
        summary = summary,
        problemStatement = problemStatement,
        postCount = postCount,
        isWatched = isWatched,
        date = dateIso?.let { Instant.parse(it) }
    )
