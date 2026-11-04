package com.example.problemfinder.data.opportunities.impl.util

import com.example.problemfinder.core.model.search.OpportunitySearchQuery
import com.example.problemfinder.core.model.search.OpportunitySearchSort
import com.example.problemfinder.data.opportunities.impl.model.dto.*
import org.junit.Assert.*
import org.junit.Test

class OpportunitySearchMapperTest {
    @Test
    fun normalizedFilterIdentityAndServerOrderSurviveMapping() {
        val query = OpportunitySearchQuery(" build failure ", listOf(" Reddit ", "reddit", "GitHub"),
            sort = OpportunitySearchSort.LARGEST, limit = 2)
        val request = query.toSearchRequest(2)
        assertEquals(query.cacheKey(), query.copy(sources = listOf("github", "reddit")).cacheKey())
        val response = OpportunitySearchResponseDto(
            data = listOf(hit(50), hit(7)),
            total = 8,
            appliedFilters = OpportunitySearchAppliedFiltersDto("build failure", listOf("REDDIT", "GitHub"),
                emptyList(), 1, null, "largest"),
            pagination = OpportunitySearchPaginationDto(2, 2, 2, true, 4),
        )
        val page = response.toSearchPage(request)
        assertEquals(listOf(50, 7), page.items.map { it.id })
        assertEquals(4, page.nextOffset)
        assertThrows(IllegalArgumentException::class.java) {
            response.copy(pagination = response.pagination.copy(nextOffset = 2)).toSearchPage(request)
        }
        assertThrows(IllegalArgumentException::class.java) {
            response.toSearchPage(request.copy(sort = "newest"))
        }
    }

    private fun hit(id: Int) = OpportunitySearchHitDto(
        id, "Name", "Summary", "Problem", 1, isWatched = false,
    )
}
