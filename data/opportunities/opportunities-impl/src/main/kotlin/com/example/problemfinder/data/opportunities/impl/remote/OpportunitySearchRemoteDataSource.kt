package com.example.problemfinder.data.opportunities.impl.remote

import com.example.problemfinder.data.opportunities.impl.model.OpportunitiesApiConfig
import com.example.problemfinder.data.opportunities.impl.model.dto.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType
import javax.inject.Inject

class OpportunitySearchRemoteDataSource @Inject constructor(
    private val client: HttpClient,
    config: OpportunitiesApiConfig,
) {
    private val base = config.baseUrl.trimEnd('/') + "/clusters/search"

    suspend fun fetchResults(request: OpportunitySearchRequestDto, accessToken: String): OpportunitySearchResponseDto =
        client.post("$base/query") {
            bearerAuth(accessToken)
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun prepareDraft(request: OpportunitySearchInterpretRequestDto, accessToken: String): OpportunitySearchDraftDto =
        client.post("$base/interpret") {
            bearerAuth(accessToken)
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun options(accessToken: String): OpportunitySearchOptionsDto =
        client.get("$base/options") { bearerAuth(accessToken) }.body()
}
