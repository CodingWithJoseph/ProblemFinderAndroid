package com.example.problemfinder.data.browse.impl.util

import com.example.problemfinder.data.browse.impl.model.dto.BrowsePageDto
import com.example.problemfinder.data.browse.impl.model.dto.DiscoveryClusterDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import kotlin.time.Instant

class BrowseMapperTest {
    @Test
    fun `timestamp offset and fractional precision survive page mapping`() {
        val page = BrowsePageDto(
            clusters = listOf(cluster("2026-10-10T14:03:04.123456+02:30")),
            nextCursor = "opaque-page-token"
        ).toModel()

        assertEquals(Instant.parse("2026-10-10T11:33:04.123456Z"), page.clusters.single().date)
        assertEquals("opaque-page-token", page.nextCursor)
    }

    @Test
    fun `missing optional data stays missing after mapping`() {
        val model = cluster(null).toModel()

        assertNull(model.name)
        assertNull(model.summary)
        assertNull(model.problemStatement)
        assertNull(model.date)
    }

    @Test
    fun `malformed timestamp fails mapping`() {
        assertThrows(IllegalArgumentException::class.java) {
            cluster("not-a-timestamp").toModel()
        }
    }

    private fun cluster(date: String?) = DiscoveryClusterDto(
        id = 30,
        name = null,
        summary = null,
        problemStatement = null,
        postCount = 3,
        isWatched = false,
        date = date
    )
}
