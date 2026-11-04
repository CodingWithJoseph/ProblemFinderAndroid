package com.example.problemfinder.data.opportunities.impl.util

import com.example.problemfinder.core.model.opportunity.Opportunity
import com.example.problemfinder.core.model.opportunity.toOpportunityCategory
import com.example.problemfinder.core.model.search.*
import com.example.problemfinder.data.opportunities.impl.model.dto.*
import kotlinx.serialization.json.Json
import kotlin.time.Instant

internal fun OpportunitySearchQuery.toSearchRequest(offset: Int): OpportunitySearchRequestDto {
    require(offset in 0..1_000_000)
    val normalized = normalized()
    return OpportunitySearchRequestDto(
        query = normalized.query.ifEmpty { null },
        sources = normalized.sources,
        communities = normalized.communities,
        minPosts = normalized.minPosts,
        observedAfter = normalized.observedAfter?.toString(),
        sort = normalized.sort.name.lowercase(),
        limit = normalized.limit,
        offset = offset,
    )
}

internal fun OpportunitySearchResponseDto.toSearchPage(request: OpportunitySearchRequestDto): OpportunitySearchPage {
    val p = pagination
    require(total >= 0 && p.offset == request.offset && p.limit == request.limit)
    require(p.returned == data.size && p.returned <= p.limit)
    require(p.hasMore == (p.offset + p.returned < total))
    require(if (p.hasMore) p.returned > 0 && p.nextOffset == p.offset + p.limit else p.nextOffset == null)
    val applied = appliedFilters.run {
        OpportunitySearchRequestDto(query, sources, communities, minPosts, observedAfter, sort, p.limit).toQuery()
    }
    require(applied == request.toQuery())
    return OpportunitySearchPage(
        items = data.map {
            Opportunity(
                id = it.id,
                name = requireNotNull(it.name).trim(),
                summary = requireNotNull(it.summary).trim(),
                problemStatement = requireNotNull(it.problemStatement).trim(),
                postCount = it.postCount,
                isWatched = it.isWatched,
                date = it.dateRangeEnd?.let(Instant::parse),
                category = (it.category ?: "uncategorized").toOpportunityCategory(),
            )
        },
        appliedQuery = applied,
        total = total,
        nextOffset = p.nextOffset,
    )
}

private fun OpportunitySearchRequestDto.toQuery() = OpportunitySearchQuery(
    query = query.orEmpty(),
    sources = sources,
    communities = communities,
    minPosts = minPosts,
    observedAfter = observedAfter?.let(Instant::parse),
    sort = OpportunitySearchSort.valueOf(sort.uppercase()),
    limit = limit,
).normalized()

internal fun OpportunitySearchDraftDto.toSearchDraft() = OpportunitySearchDraft(
    draft.toQuery(), confirmation, assumptions, unsupported, clarificationQuestion,
    needsClarification, needsConfirmation, fallbackUsed,
)

internal fun OpportunitySearchQuery.cacheKey(): String = Json.encodeToString(toSearchRequest(0))
