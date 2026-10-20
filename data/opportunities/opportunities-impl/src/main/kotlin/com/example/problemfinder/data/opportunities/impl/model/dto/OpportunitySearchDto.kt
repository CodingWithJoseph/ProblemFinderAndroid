package com.example.problemfinder.data.opportunities.impl.model.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * TODO [SEARCH-1B] Review this EXISTING request DTO against backend app/search_contract.py.
 * POST /clusters/search/query expects this structured JSON, not Browse query parameters.
 * Verify @SerialName maps minPosts -> min_posts and observedAfter -> observed_after. Wire sort is
 * "relevance", "newest" or "largest"; offset starts at zero and later comes from next_offset.
 * DTO defaults mirror allowed server defaults; normalization/validation still belongs in the
 * app query/use-case/mapper path. Unknown industry/category fields are rejected by the server.
 * NEXT: implement OpportunitySearchQuery.toSearchRequest(offset) without calling HTTP yet.
 */
@Serializable
data class OpportunitySearchRequestDto(
    val query: String? = null,
    val sources: List<String> = emptyList(),
    val communities: List<String> = emptyList(),
    @SerialName("min_posts") val minPosts: Int = 1,
    @SerialName("observed_after") val observedAfter: String? = null,
    val sort: String = "relevance",
    val limit: Int = 20,
    val offset: Int = 0,
)

@Serializable
/**
 * SEARCH-1B response envelope for POST /clusters/search/query.
 * data contains legacy search hits, total is the matching result count, applied_filters is canonical
 * filter metadata, and pagination owns limit/offset/returned/has_more/next_offset. Audit every layer.
 * GET /clusters/search does not return this same envelope and hardcodes largest sorting.
 */
data class OpportunitySearchResponseDto(
    val data: List<OpportunitySearchHitDto>,
    val total: Int,
    @SerialName("applied_filters") val appliedFilters: OpportunitySearchAppliedFiltersDto,
    val pagination: OpportunitySearchPaginationDto,
)

/**
 * TODO [SEARCH-1B] Resolve this contract gap BEFORE trying to reuse OpportunityDto.toModel().
 * Backend _serialize_discovery_cluster supplies id/name/summary/problemStatement, snake_case counts,
 * dates and membership. Unlike the Browse card response, it currently supplies NO category.
 *
 * Recommended contract work: extend that search serializer/response contract to return the same
 * category key used by Browse, add its DTO property, deploy it and update fixtures. If you choose
 * a different explicit legacy policy, document what absence means first. UNCATEGORIZED is a real
 * category value, not automatic evidence that a missing backend field was successfully classified.
 * Likewise, nullable legacy name/summary/problemStatement must fail strict mapping when unusable;
 * do not silently fill dummy titles, empty text, or false membership. is_watched is intentionally
 * required here. Preserve accurate counts and use date_range_end for the card's latest activity.
 * Search-specific source/community metadata need only enter the app model if the UI actually uses it.
 * Finish check: a fixture satisfying the agreed contract maps to the same trustworthy Opportunity
 * model as Browse; a missing required field cannot masquerade as a successful result card.
 */
@Serializable
data class OpportunitySearchHitDto(
    val id: Int,
    val name: String? = null,
    val summary: String? = null,
    val problemStatement: String? = null,
    @SerialName("post_count") val postCount: Int,
    @SerialName("date_range_start") val dateRangeStart: String? = null,
    @SerialName("date_range_end") val dateRangeEnd: String? = null,
    val sources: List<String> = emptyList(),
    val subreddits: List<String> = emptyList(),
    @SerialName("is_watched") val isWatched: Boolean,
)

@Serializable
data class OpportunitySearchAppliedFiltersDto(
    val query: String? = null,
    val sources: List<String> = emptyList(),
    val communities: List<String> = emptyList(),
    @SerialName("min_posts") val minPosts: Int,
    @SerialName("observed_after") val observedAfter: String? = null,
    val sort: String,
)

@Serializable
data class OpportunitySearchPaginationDto(
    val limit: Int,
    val offset: Int,
    val returned: Int,
    @SerialName("has_more") val hasMore: Boolean,
    @SerialName("next_offset") val nextOffset: Int? = null,
)

@Serializable
data class OpportunitySearchInterpretRequestDto(
    val message: String,
    @SerialName("current_draft") val currentDraft: OpportunitySearchRequestDto? = null,
)

/** /search/agent adds public steps/stop_reason; unknown keys are ignored by the shared decoder. */
@Serializable
data class OpportunitySearchDraftDto(
    val draft: OpportunitySearchRequestDto,
    val confirmation: String,
    val assumptions: List<String>,
    val unsupported: List<String>,
    @SerialName("clarification_question") val clarificationQuestion: String? = null,
    @SerialName("needs_clarification") val needsClarification: Boolean,
    @SerialName("needs_confirmation") val needsConfirmation: Boolean,
    @SerialName("fallback_used") val fallbackUsed: Boolean,
)
