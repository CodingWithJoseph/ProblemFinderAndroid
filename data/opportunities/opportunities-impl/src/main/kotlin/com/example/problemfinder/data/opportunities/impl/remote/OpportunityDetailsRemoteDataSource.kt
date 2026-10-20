package com.example.problemfinder.data.opportunities.impl.remote

import com.example.problemfinder.data.opportunities.impl.model.OpportunitiesApiConfig
import com.example.problemfinder.data.opportunities.impl.model.dto.OpportunityDetailsDto
import io.ktor.client.HttpClient
import javax.inject.Inject

class OpportunityDetailsRemoteDataSource @Inject constructor(
    private val client: HttpClient,
    private val config: OpportunitiesApiConfig,
) {
    suspend fun fetchDetails(clusterId: Int, accessToken: String, page: Int = 1, pageSize: Int = 20): OpportunityDetailsDto {
        // TODO [DETAILS-3A] Implement only the HTTP boundary after contract/cache work is ready.
        // 1. Use the injected client and config.baseUrl (trim its trailing slash) for GET /clusters/{id}.
        // 2. Send bearerAuth(accessToken). Validate clusterId/page are positive and pageSize is 1..200.
        // 3. Send query parameters exactly as backend get_cluster expects: "page" and camelCase "pageSize".
        //    Page 1 is the first page. Do not send Browse cursor/limit or Search offset here.
        // 4. Decode body<OpportunityDetailsDto>() and return the DTO. BrowseRemoteDataSource demonstrates
        //    the required Ktor imports/get/parameter/body mechanics with the existing configured client.
        // 5. Let failures propagate. Before general HTTP error mapping, preserve enough information to
        //    represent 404 as a details-specific not-found outcome; REQUEST_REJECTED alone loses that detail.
        // No cache writes, DTO -> model conversion, auth-session lookup, automatic paging or UI changes here.
        // Finish check with MockEngine: exact path/token/parameters, correct nested DTO decoding, HTTP 404
        // and malformed JSON fail. NEXT: repository refresh validates the returned ID and stores the page.
        throw UnsupportedOperationException("Complete DETAILS-3a: authenticated detail request")
    }
}
