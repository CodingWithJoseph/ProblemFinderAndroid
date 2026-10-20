package com.example.problemfinder.core.model.search

import com.example.problemfinder.core.model.opportunity.Opportunity
import kotlin.time.Instant

/** Search accepts these three orders; Browse's OLDEST/SMALLEST are not valid search wire values. */
enum class OpportunitySearchSort { RELEVANCE, NEWEST, LARGEST }

/**
 * TODO [SEARCH-1A] Complete app-side query rules; these model files already exist.
 * This object is the immutable identity of an EXECUTED result set. The editable text field keeps a
 * separate draft value. Neither Room annotations nor JSON spellings belong in the app query model.
 *
 * Write next:
 * 1. Choose one reusable normalization/validation path called before executing requests or building
 *    cache keys. Trim query text; normalize/deduplicate filter values consistently.
 * 2. Match backend ClusterSearchQuery: non-blank query length 2..500; minPosts 1..100000; limit 1..50;
 *    each source/community list at most 20 values and each non-empty value at most 100 characters.
 * 3. Keep RELEVANCE, NEWEST and LARGEST as the only search orders. OLDEST/SMALLEST are Browse-only.
 * 4. Treat observedAfter as an optional Instant; the mapper serializes it as ISO text for the server.
 * 5. Decide the input UX explicitly: the backend allows filter-only queries (null text). Distinguish
 *    that from accidentally submitting an untouched blank/unfiltered screen; do not auto-search on entry.
 *
 * offset belongs to saved page progress, not filter identity. Industry/discovery-category filters
 * are not accepted by the current backend; add that contract before implementing those controls.
 * Finish check: equivalent normalized input is stable, bad limits/text/filters are rejected, and
 * editing a draft cannot mutate a query captured by an already-running request.
 * NEXT: SEARCH-1B DTO review, then SEARCH-1C mapping tests. No Room or live request yet.
 */
data class OpportunitySearchQuery(
    val query: String = "",
    val sources: List<String> = emptyList(),
    val communities: List<String> = emptyList(),
    val minPosts: Int = 1,
    val observedAfter: Instant? = null,
    val sort: OpportunitySearchSort = OpportunitySearchSort.RELEVANCE,
    val limit: Int = 20,
)

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

/**
 * TODO [SEARCH-8] Add assistant interpretation only after manual structured Search works end to end.
 * This is a proposal, not a result page. /clusters/search/agent returns filters plus confirmation,
 * assumptions, unsupported requests, optional clarification and fallback flags; it does not run SQL.
 * Show that proposal to the user. A clarification needs another turn; a usable draft needs an explicit
 * ConfirmDraft action before the existing search execution path runs. fallbackUsed is a usable server
 * fallback result, not a failed search. Never set result-cache hasSynced merely because a draft exists.
 */
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
