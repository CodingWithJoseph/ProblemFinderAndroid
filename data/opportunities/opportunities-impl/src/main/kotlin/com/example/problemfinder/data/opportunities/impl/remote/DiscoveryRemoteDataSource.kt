package com.example.problemfinder.data.opportunities.impl.remote

import com.example.problemfinder.data.opportunities.impl.model.OpportunitiesApiConfig
import com.example.problemfinder.data.opportunities.impl.model.dto.DiscoveryDashboardDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import javax.inject.Inject

class DiscoveryRemoteDataSource @Inject constructor(
    private val client: HttpClient,
    private val config: OpportunitiesApiConfig
) {
    suspend fun fetchDashboard(
        accessToken: String
    ): DiscoveryDashboardDto = client.get("${config.baseUrl.trimEnd('/')}/clusters/discovery") {
        bearerAuth(accessToken)
    }.body()
}
