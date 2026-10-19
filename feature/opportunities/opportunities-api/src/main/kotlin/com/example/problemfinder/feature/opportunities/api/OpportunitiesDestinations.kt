package com.example.problemfinder.feature.opportunities.api

/** Public route names shared with the app host without depending on screen implementation. */
object OpportunitiesDestinations {
    const val DISCOVERY = "discovery_dashboard"
    const val BROWSE_ALL = "browse_all"

    const val CATEGORY_ARGUMENT = "category"

    const val BROWSE_ROUTE = "$BROWSE_ALL?$CATEGORY_ARGUMENT={$CATEGORY_ARGUMENT}"
    /** Expects a category enum name; the destination defaults unknown/missing values to ALL. */
    fun browseRoute(category: String): String = "$BROWSE_ALL?$CATEGORY_ARGUMENT=$category"
}
