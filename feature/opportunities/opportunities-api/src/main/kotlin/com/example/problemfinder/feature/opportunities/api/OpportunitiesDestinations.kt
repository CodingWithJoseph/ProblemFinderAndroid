package com.example.problemfinder.feature.opportunities.api

object OpportunitiesDestinations {
    const val DISCOVERY = "discovery_dashboard"
    const val BROWSE_ALL = "browse_all"

    const val CATEGORY_ARGUMENT = "category"

    const val BROWSE_ROUTE = "$BROWSE_ALL?$CATEGORY_ARGUMENT={$CATEGORY_ARGUMENT}"
    fun browseRoute(category: String): String = "$BROWSE_ALL?$CATEGORY_ARGUMENT=$category"
}
