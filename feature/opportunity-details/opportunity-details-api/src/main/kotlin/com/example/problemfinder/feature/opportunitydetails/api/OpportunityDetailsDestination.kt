package com.example.problemfinder.feature.opportunitydetails.api

object OpportunityDetailsDestination {
    const val ID_ARGUMENT = "clusterId"
    const val ROUTE = "opportunity_details/{$ID_ARGUMENT}"

    fun route(clusterId: Int): String {
        require(clusterId > 0) { "Opportunity ID must be positive" }
        return "opportunity_details/$clusterId"
    }
}
