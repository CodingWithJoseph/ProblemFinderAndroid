package com.example.problemfinder.data.opportunities.impl.remote

import com.example.problemfinder.data.opportunities.impl.model.OpportunitiesApiConfig
import com.example.problemfinder.data.opportunities.impl.model.dto.OpportunitySearchDraftDto
import com.example.problemfinder.data.opportunities.impl.model.dto.OpportunitySearchInterpretRequestDto
import com.example.problemfinder.data.opportunities.impl.model.dto.OpportunitySearchRequestDto
import com.example.problemfinder.data.opportunities.impl.model.dto.OpportunitySearchResponseDto
import io.ktor.client.HttpClient
import javax.inject.Inject

class OpportunitySearchRemoteDataSource @Inject constructor(
    private val client: HttpClient,
    private val config: OpportunitiesApiConfig,
) {
    suspend fun fetchResults(request: OpportunitySearchRequestDto, accessToken: String): OpportunitySearchResponseDto {
        // TODO [SEARCH-3A] Implement the HTTP request only; the repository will validate/store its response.
        // 1. Build POST {config.baseUrl}/clusters/search/query using the injected shared HttpClient.
        // 2. Attach bearerAuth(accessToken), JSON content type, and setBody(request). The serialization
        //    plugin applies @SerialName; do not manually concatenate JSON or expose credentials in logs.
        // 3. Decode/return OpportunitySearchResponseDto. Keep DTO -> app mapping and every DAO call outside
        //    this remote source. Let HTTP/decode failures reach the repository's error classification.
        // 4. Use the structured POST endpoint: GET /clusters/search fixes sort to largest and lacks this
        //    pagination envelope. Do not send Browse cursor/category or query-string fields for this body.
        // Finish check with MockEngine: POST path/token/body spellings, all allowed sorts, first offset 0,
        // next-page offset supplied by the caller, HTTP failure and malformed JSON behavior.
        throw UnsupportedOperationException("Complete SEARCH-3a: structured search request")
    }

    suspend fun prepareDraft(request: OpportunitySearchInterpretRequestDto, accessToken: String): OpportunitySearchDraftDto {
        // TODO [SEARCH-8] POST /clusters/search/agent. It returns a confirmable draft, never results.
        // The backend already supports agent/interpreter fallback. Preserve fallback_used and
        // clarification/confirmation flags; do not invent a second client-side interpretation.
        // The interpretation endpoints do not execute search. Confirming calls fetchResults later.
        // Check: preparing a draft issues zero /search/query calls and zero result-cache writes.
        throw UnsupportedOperationException("Complete SEARCH-8: prepare a search draft")
    }
}
