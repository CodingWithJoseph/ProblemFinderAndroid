package com.example.problemfinder.data.opportunities.impl.util

import com.example.problemfinder.core.model.opportunity.OpportunityCategory
import com.example.problemfinder.data.opportunities.impl.model.dto.BrowsePageDto
import com.example.problemfinder.data.opportunities.impl.model.dto.OpportunityDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import kotlin.time.Instant

class OpportunityMapperTest {
    @Test fun `timestamp precision and cursor survive mapping`() {
        val page = BrowsePageDto(listOf(card().copy(date = "2026-10-10T14:03:04.123456+02:30")), "next").toModel()
        assertEquals(Instant.parse("2026-10-10T11:33:04.123456Z"), page.clusters.single().date)
        assertEquals("next", page.nextCursor)
    }
    @Test fun `only activity date may be absent`() { assertNull(card().toModel().date) }
    @Test fun `all categories survive cache round trip`() {
        for (category in OpportunityCategory.entries) {
            val model = card().copy(category = category.name.lowercase()).toModel()
            assertEquals(category, model.category)
            assertEquals(model, model.toEntity("user-one").toModel())
        }
    }
    @Test fun `invalid content cannot enter the model or cache`() {
        val invalid = listOf(
            card().copy(id = 0), card().copy(id = -1), card().copy(postCount = -1),
            card().copy(name = " "), card().copy(summary = "\t\n"),
            card().copy(problemStatement = ""), card().copy(category = "future_category"),
            card().copy(date = "invalid"),
        )
        invalid.forEach { dto -> assertThrows(IllegalArgumentException::class.java) { dto.toModel() } }
    }
    @Test fun `surrounding whitespace is removed at the network boundary`() {
        val model = card().copy(name = "  Name ", summary = " Summary\n", problemStatement = "\tProblem ").toModel()
        assertEquals("Name", model.name)
        assertEquals("Summary", model.summary)
        assertEquals("Problem", model.problemStatement)
    }
    private fun card() = OpportunityDto(30, "Name", "Summary", "Problem", 3, "uncategorized", false, null)
}
