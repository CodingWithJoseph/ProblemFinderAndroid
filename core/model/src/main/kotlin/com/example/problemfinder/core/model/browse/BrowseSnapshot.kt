package com.example.problemfinder.core.model.browse

import com.example.problemfinder.core.model.opportunity.Opportunity

/**
 * Cached results for the observed account and Browse query. cacheReady distinguishes an account
 * transition from the first database read; hasSynced distinguishes a fetched empty page from a
 * query never fetched. Neither flag guarantees that the cached content is still fresh.
 */
data class BrowseSnapshot(
    val userId: String? = null,
    val clusters: List<Opportunity> = emptyList(),
    val nextCursor: String? = null,
    val hasSynced: Boolean = false,
    val cacheReady: Boolean = true
)
