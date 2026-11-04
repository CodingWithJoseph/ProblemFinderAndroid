package com.example.problemfinder.data.opportunities.impl.model.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
data class OpportunitySearchOptionsDto(val sources: List<String>, val communities: List<String>)

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
    val category: String? = null,
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
