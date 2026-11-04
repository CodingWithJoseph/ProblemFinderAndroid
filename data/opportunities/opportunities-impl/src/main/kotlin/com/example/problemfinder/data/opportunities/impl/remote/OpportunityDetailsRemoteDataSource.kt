package com.example.problemfinder.data.opportunities.impl.remote

import com.example.problemfinder.data.opportunities.impl.model.OpportunitiesApiConfig
import com.example.problemfinder.data.opportunities.impl.model.dto.OpportunityDetailsDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import javax.inject.Inject

class OpportunityDetailsRemoteDataSource @Inject constructor(
    private val client: HttpClient,
    private val config: OpportunitiesApiConfig,
) {
    suspend fun fetchDetails(
        clusterId: Int,
        accessToken: String,
        page: Int = 1,
        pageSize: Int = 20
    ): OpportunityDetailsDto {
        require(page >= 1) {"page must be least 1: $page"}
        require(clusterId > 0) {"clusterId must be nonnegative: $clusterId"}
        require(pageSize in 1..200) {"pageSize be in between (inclusive) 1-200: $pageSize"}

        val url = "${config.baseUrl.trimEnd('/')}/clusters/${clusterId}"
        return client.get(url) {
            bearerAuth(accessToken)
            parameter("page", page)
            parameter("pageSize", pageSize)
        }.body()
    }
}
