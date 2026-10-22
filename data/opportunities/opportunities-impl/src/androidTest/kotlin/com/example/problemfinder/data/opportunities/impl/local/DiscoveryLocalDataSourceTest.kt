package com.example.problemfinder.data.opportunities.impl.local

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.problemfinder.core.common.shared.OpportunityCategoryFilter
import com.example.problemfinder.core.common.shared.SortType
import com.example.problemfinder.core.model.browse.BrowsePage
import com.example.problemfinder.core.model.opportunity.OpportunityCategory
import com.example.problemfinder.core.model.opportunity.Opportunity
import com.example.problemfinder.core.model.discovery.DiscoveryCollection
import com.example.problemfinder.core.model.discovery.DiscoveryDashboard
import com.example.problemfinder.core.model.discovery.DiscoverySnapshot
import com.example.problemfinder.data.opportunities.impl.db.dao.BrowseDao
import com.example.problemfinder.data.opportunities.impl.db.dao.DiscoveryDao
import com.example.problemfinder.data.opportunities.impl.db.OpportunitiesDatabase
import com.example.problemfinder.data.opportunities.impl.model.entity.DiscoveryCollectionItemEntity
import com.example.problemfinder.data.opportunities.impl.util.toModel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DiscoveryLocalDataSourceTest {
    private lateinit var database: OpportunitiesDatabase
    private lateinit var browseDao: BrowseDao
    private lateinit var discoveryDao: DiscoveryDao
    private lateinit var source: DiscoveryLocalDataSource

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder<OpportunitiesDatabase>(context)
            .setDriver(AndroidSQLiteDriver())
            .build()
        browseDao = database.browseDao()
        discoveryDao = database.discoveryDao()
        source = DiscoveryLocalDataSource(database, database.opportunityDao(), discoveryDao)
    }

    @After
    fun tearDown() { database.close() }

    @Test
    fun missingDashboardFinishesCacheReadWithoutClaimingASync() = runBlocking {
        val result = snapshot()
        assertEquals(USER, result.userId)
        assertTrue(result.cacheReady)
        assertFalse(result.hasSynced)
        assertEquals(EMPTY_DASHBOARD, result.dashboard)
    }

    @Test
    fun joinedQueryPreservesFeaturedCardFixedCollectionOrderAndEmptyCollections() = runBlocking {
        val featured = cluster(42)
        val build = DiscoveryCollection(OpportunityCategory.BUILD_AND_TOOLING, listOf(featured, cluster(17)))
        val data = DiscoveryCollection(OpportunityCategory.DATA_AND_APIS, listOf(cluster(8, OpportunityCategory.DATA_AND_APIS)))
        val workflow = DiscoveryCollection(OpportunityCategory.WORKFLOW_AND_AUTOMATION, emptyList())
        source.replaceDashboard(USER, DiscoveryDashboard(featured, listOf(workflow, data, build)))

        val result = snapshot()
        assertEquals(DiscoveryDashboard(featured, listOf(build, data, workflow)), result.dashboard)
        assertTrue(result.hasSynced)
        assertTrue(result.cacheReady)
    }

    @Test
    fun replacingWithEmptyDashboardRemovesOldMembershipsAndRetainsBrowseContent() = runBlocking {
        val original = cluster(42)
        val browseSource = BrowseLocalDataSource(database, browseDao, database.opportunityDao())
        browseSource.replaceFirstPage(USER, OpportunityCategoryFilter.ALL, SortType.NEWEST, BrowsePage(listOf(original), null))
        source.replaceDashboard(USER, dashboard(original))
        source.replaceDashboard(USER, EMPTY_DASHBOARD)

        val result = snapshot()
        assertEquals(EMPTY_DASHBOARD, result.dashboard)
        assertTrue(result.hasSynced)
        val browse = withTimeout(10_000) {
            browseSource.observeBrowse(USER, OpportunityCategoryFilter.ALL, SortType.NEWEST).first()
        }
        assertEquals(listOf(original), browse.clusters)
    }

    @Test
    fun membershipUsesOwningCollectionEvenWhenClusterClassificationDiffers() = runBlocking {
        val classifiedAsData = cluster(8, OpportunityCategory.DATA_AND_APIS)
        val expected = DiscoveryDashboard(
            null,
            listOf(DiscoveryCollection(OpportunityCategory.BUILD_AND_TOOLING, listOf(classifiedAsData))),
        )
        source.replaceDashboard(USER, expected)
        assertEquals(expected, snapshot().dashboard)
    }

    @Test
    fun sameClusterIdInDifferentAccountsKeepsContentAndWatchedStatusSeparate() = runBlocking {
        val first = cluster(42).copy(name = "First account", isWatched = true)
        val second = cluster(42).copy(name = "Second account", isWatched = false)
        source.replaceDashboard(USER, dashboard(first))
        source.replaceDashboard(OTHER_USER, dashboard(second))

        assertEquals(dashboard(first), snapshot(USER).dashboard)
        assertEquals(dashboard(second), snapshot(OTHER_USER).dashboard)
        source.replaceDashboard(OTHER_USER, EMPTY_DASHBOARD)
        assertEquals(dashboard(first), snapshot(USER).dashboard)
    }

    @Test
    fun failedMembershipWriteRollsBackContentMetadataAndArrangement() = runBlocking {
        source.replaceDashboard(USER, dashboard(cluster(42)))
        val before = snapshot()
        val failingDao = object : DiscoveryDao by discoveryDao {
            override suspend fun insertCollectionItems(items: List<DiscoveryCollectionItemEntity>) {
                discoveryDao.insertCollectionItems(items)
                error("Injected membership failure")
            }
        }
        val failingSource = DiscoveryLocalDataSource(database, database.opportunityDao(), failingDao)
        val replacement = DiscoveryDashboard(
            cluster(17),
            listOf(
                DiscoveryCollection(
                    OpportunityCategory.DATA_AND_APIS,
                    listOf(cluster(42).copy(name = "Changed"), cluster(17)),
                )
            ),
        )
        val error = runCatching { failingSource.replaceDashboard(USER, replacement) }.exceptionOrNull()
        assertTrue(error is IllegalStateException)
        assertEquals("Injected membership failure", error?.message)
        assertEquals(before, snapshot())
    }

    @Test
    fun sharedClusterUpdateReemitsFeaturedAndCollectionWatchedStatus() = runBlocking {
        val original = cluster(42)
        source.replaceDashboard(USER, dashboard(original))
        coroutineScope {
            val emissions = Channel<DiscoverySnapshot>(Channel.UNLIMITED)
            val collector = launch {
                source.observeDashboard(USER).collect { emissions.send(it) }
            }
            try {
                val before = withTimeout(10_000) { emissions.receive() }
                assertFalse(requireNotNull(before.dashboard.featuredCluster).isWatched)
                val watched = original.copy(isWatched = true)
                database.opportunityDao().upsertOpportunities(listOf(watched.toModel(USER)))
                val after = withTimeout(10_000) { emissions.receive() }
                assertEquals(dashboard(watched), after.dashboard)
                assertNull(after.dashboard.featuredCluster?.date)
            } finally {
                collector.cancelAndJoin()
                emissions.close()
            }
        }
    }

    private suspend fun snapshot(userId: String = USER): DiscoverySnapshot = withTimeout(10_000) {
        source.observeDashboard(userId).first()
    }

    private fun dashboard(cluster: Opportunity) = DiscoveryDashboard(
        cluster,
        listOf(DiscoveryCollection(cluster.category, listOf(cluster))),
    )

    private fun cluster(id: Int, category: OpportunityCategory = OpportunityCategory.BUILD_AND_TOOLING) =
        Opportunity(
            id = id,
            name = "Opportunity $id",
            summary = "A complete opportunity summary for the preview.",
            problemStatement = "Problem $id",
            postCount = id,
            isWatched = false,
            date = null,
            category = category,
        )

    companion object {
        private const val USER = "user-one"
        private const val OTHER_USER = "user-two"
        private val EMPTY_DASHBOARD = DiscoveryDashboard(null, emptyList())
    }
}
