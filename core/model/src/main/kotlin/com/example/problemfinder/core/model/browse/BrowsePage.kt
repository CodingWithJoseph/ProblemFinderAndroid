package com.example.problemfinder.core.model.browse

import com.example.problemfinder.core.model.opportunity.Opportunity

/** A mapped server page; nextCursor is opaque and null marks the end of pagination. */
data class BrowsePage(
    val clusters: List<Opportunity>,
    val nextCursor: String? = null
)
