package com.example.problemfinder.data.opportunities.impl.remote

import com.example.problemfinder.data.opportunities.impl.model.OpportunitiesApiConfig
import com.example.problemfinder.data.opportunities.impl.model.dto.OpportunityWatchDto
import com.example.problemfinder.data.opportunities.impl.model.dto.WatchOpportunityBody
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import javax.inject.Inject

class OpportunityActionsRemoteDataSource @Inject constructor(
    private val client: HttpClient,
    private val config: OpportunitiesApiConfig,
) {
    suspend fun setWatched(clusterId: Int, watched: Boolean, accessToken: String): OpportunityWatchDto {
        val url = "${config.baseUrl.trimEnd('/')}/pipeline/watch"
        return if (watched) client.post(url) {
            bearerAuth(accessToken)
            contentType(ContentType.Application.Json)
            setBody(WatchOpportunityBody(clusterId.toString()))
        }.body() else client.delete("$url/$clusterId") {
            bearerAuth(accessToken)
        }.body()
    }
}
