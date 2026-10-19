package com.example.problemfinder.feature.opportunitydetails.api

/** Route contract for opening a selected opportunity; the current destination renders a placeholder. */
object OpportunityDetailsDestination {
    const val ID_ARGUMENT = "clusterId"
    const val ROUTE = "opportunity_details/{$ID_ARGUMENT}"

    /** Rejects invalid IDs when callers construct a route; the route carries identity, not a data object. */
    fun route(clusterId: Int): String {
        require(clusterId > 0) { "Opportunity ID must be positive" }
        return "opportunity_details/$clusterId"
    }
}
