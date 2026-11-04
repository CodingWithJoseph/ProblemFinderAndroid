package com.example.problemfinder.feature.details.api

import java.net.URLEncoder

/** Routes carry identity only; a post can also be opened independently of the overview. */
object OpportunityDetailsDestination {
    const val ID_ARGUMENT = "clusterId"
    const val ROUTE = "opportunity_details/{$ID_ARGUMENT}"
    const val POST_ARGUMENT = "postId"
    const val EVIDENCE_ROUTE = "opportunity_details/{$ID_ARGUMENT}/evidence"
    const val POST_ROUTE = "opportunity_details/{$ID_ARGUMENT}/posts/{$POST_ARGUMENT}"

    fun evidenceRoute(clusterId: Int): String = route(clusterId) + "/evidence"
    fun postRoute(clusterId: Int, postId: String): String {
        require(postId.isNotBlank())
        val segment = URLEncoder.encode(postId, Charsets.UTF_8.name()).replace("+", "%20")
        return route(clusterId) + "/posts/" + segment
    }

    /** Rejects invalid IDs when callers construct a route; the route carries identity, not a data object. */
    fun route(clusterId: Int): String {
        require(clusterId > 0) { "Opportunity ID must be positive" }
        return "opportunity_details/$clusterId"
    }
}
