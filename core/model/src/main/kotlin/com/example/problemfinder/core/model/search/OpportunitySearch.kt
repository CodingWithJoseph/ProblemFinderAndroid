package com.example.problemfinder.core.model.search

import com.example.problemfinder.core.model.opportunity.Opportunity
import kotlin.time.Instant

/** Search accepts these three orders; Browse's OLDEST/SMALLEST are not valid search wire values. */
enum class OpportunitySearchSort { RELEVANCE, NEWEST, LARGEST }

/** Immutable result-set identity. Offset is stored separately as page progress. */
data class OpportunitySearchQuery(
    val query: String = "",
    val sources: List<String> = emptyList(),
    val communities: List<String> = emptyList(),
    val minPosts: Int = 1,
    val observedAfter: Instant? = null,
    val sort: OpportunitySearchSort = OpportunitySearchSort.RELEVANCE,
    val limit: Int = 20,
) {
    fun normalized(): OpportunitySearchQuery {
        val text = query.trim()
        require(text.isEmpty() || text.codePointCount(0, text.length) in 2..500)
        require(minPosts in 1..100_000 && limit in 1..50)
        fun normalize(values: List<String>): List<String> {
            require(values.size <= 20)
            return values.map { it.trim().lowercase() }
                .filter { it.isNotEmpty() && it != "all" }
                .onEach { require(it.length <= 100) }
                .distinct().sorted()
        }
        return copy(query = text, sources = normalize(sources), communities = normalize(communities))
    }
}

data class OpportunitySearchOptions(val sources: List<String>, val communities: List<String>)

/**
 * SEARCH-1: a validated backend page, before persistence.
 * items retain backend relevance order; appliedQuery is the normalized filter state the server
 * executed; nextOffset is server-provided progress, not the number of deduplicated visible cards.
 * Add checks for non-negative total/offset and consistent page metadata in the response mapper.
 */
data class OpportunitySearchPage(
    val items: List<Opportunity>,
    val appliedQuery: OpportunitySearchQuery,
    val total: Int,
    val nextOffset: Int?,
)

/**
 * SEARCH-2: the local source emits this for ONE account and ONE normalized submitted query.
 * items accumulates that query's stored pages in server order. Read total/nextOffset/hasSynced from
 * saved query metadata; an empty list alone cannot distinguish never loaded from successful zero results.
 * cacheReady reports completion of the local read, including an empty one. It is not HTTP success.
 */
data class OpportunitySearchSnapshot(
    val query: OpportunitySearchQuery,
    val userId: String? = null,
    val items: List<Opportunity> = emptyList(),
    val total: Int = 0,
    val nextOffset: Int? = null,
    val cacheReady: Boolean = false,
    val hasSynced: Boolean = false,
)

/** A search proposal awaiting clarification or explicit confirmation. */
data class OpportunitySearchDraft(
    val query: OpportunitySearchQuery,
    val confirmation: String,
    val assumptions: List<String>,
    val unsupported: List<String>,
    val clarificationQuestion: String?,
    val needsClarification: Boolean,
    val needsConfirmation: Boolean,
    val fallbackUsed: Boolean,
)
