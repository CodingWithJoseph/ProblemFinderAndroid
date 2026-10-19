package com.example.problemfinder.core.model.browse

import com.example.problemfinder.core.model.opportunity.Opportunity

data class BrowsePage(
    val clusters: List<Opportunity>,
    val nextCursor: String? = null
)
