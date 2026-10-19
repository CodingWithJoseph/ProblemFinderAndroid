package com.example.problemfinder.data.opportunities.impl.util

import com.example.problemfinder.core.model.opportunity.OpportunityCategory
import com.example.problemfinder.data.opportunities.impl.model.entity.OpportunityEntity
import com.example.problemfinder.data.opportunities.impl.model.entity.DiscoveryCacheRow
import com.example.problemfinder.data.opportunities.impl.model.entity.DiscoveryCollectionEntity
import com.example.problemfinder.data.opportunities.impl.model.entity.DiscoveryDashboardEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscoveryCacheMapperTest {
    @Test
    fun `absent cache is ready but has not synced`() {
        val snapshot = emptyList<DiscoveryCacheRow>().toDiscoverySnapshot(USER)
        assertEquals(USER, snapshot.userId)
        assertTrue(snapshot.cacheReady)
        assertFalse(snapshot.hasSynced)
        assertNull(snapshot.dashboard.featuredCluster)
        assertTrue(snapshot.dashboard.collections.isEmpty())
    }

    @Test
    fun `saved empty dashboard remains successfully synced`() {
        val snapshot = listOf(row()).toDiscoverySnapshot(USER)
        assertTrue(snapshot.cacheReady)
        assertTrue(snapshot.hasSynced)
        assertNull(snapshot.dashboard.featuredCluster)
        assertTrue(snapshot.dashboard.collections.isEmpty())
    }

    @Test
    fun `joined rows reconstruct one featured card and preserve card and collection order`() {
        val featured = cluster(42)
        val rows = listOf(
            row(featured, "build_and_tooling", featured, 0),
            row(featured, "build_and_tooling", cluster(17), 1),
            row(featured, "data_and_apis", cluster(8, "data_and_apis"), 0),
            row(featured, "workflow_and_automation"),
        )
        val dashboard = rows.toDiscoverySnapshot(USER).dashboard
        assertEquals(featured.toModel(), dashboard.featuredCluster)
        assertEquals(
            listOf(
                OpportunityCategory.BUILD_AND_TOOLING,
                OpportunityCategory.DATA_AND_APIS,
                OpportunityCategory.WORKFLOW_AND_AUTOMATION,
            ),
            dashboard.collections.map { it.category },
        )
        assertEquals(listOf(42, 17), dashboard.collections[0].clusters.map { it.id })
        assertEquals(listOf(8), dashboard.collections[1].clusters.map { it.id })
        assertTrue(dashboard.collections[2].clusters.isEmpty())
    }

    @Test
    fun `featured card without collections survives the left join result`() {
        val featured = cluster(42)
        val dashboard = listOf(row(featured)).toDiscoverySnapshot(USER).dashboard
        assertEquals(featured.toModel(), dashboard.featuredCluster)
        assertTrue(dashboard.collections.isEmpty())
    }

    @Test
    fun `membership category remains distinct from cluster classification`() {
        val dataCluster = cluster(8, "data_and_apis")
        val dashboard = listOf(
            row(category = "build_and_tooling", member = dataCluster, position = 0)
        ).toDiscoverySnapshot(USER).dashboard
        assertNull(dashboard.featuredCluster)
        assertEquals(OpportunityCategory.BUILD_AND_TOOLING, dashboard.collections.single().category)
        assertEquals(OpportunityCategory.DATA_AND_APIS, dashboard.collections.single().clusters.single().category)
    }

    private fun row(
        featured: OpportunityEntity? = null,
        category: String? = null,
        member: OpportunityEntity? = null,
        position: Int? = null,
    ) = DiscoveryCacheRow(
        dashboard = DiscoveryDashboardEntity(USER, featured?.clusterId, hasSynced = true),
        featured = featured,
        collection = category?.let { DiscoveryCollectionEntity(USER, it) },
        cluster = member,
        position = position,
    )

    private fun cluster(id: Int, category: String = "build_and_tooling") = OpportunityEntity(
        userId = USER,
        clusterId = id,
        name = "Opportunity $id",
        summary = "A complete opportunity summary for the preview.",
        problemStatement = "Problem $id",
        postCount = id,
        category = category,
        isWatched = id == 42,
        dateIso = null,
    )

    companion object {
        private const val USER = "user-one"
    }
}
