package com.example.problemfinder.data.opportunities.impl.local

import android.content.Context
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.room3.withWriteTransaction
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.problemfinder.core.model.details.EvidenceDateCount
import com.example.problemfinder.core.model.details.EvidenceSourceCount
import com.example.problemfinder.core.model.details.OpportunityDetails
import com.example.problemfinder.core.model.details.OpportunityDetailsPage
import com.example.problemfinder.core.model.details.OpportunityDetailsSnapshot
import com.example.problemfinder.core.model.details.OpportunityEvidence
import com.example.problemfinder.data.opportunities.impl.db.OpportunitiesDatabase
import com.example.problemfinder.data.opportunities.impl.db.dao.OpportunityDetailsDao
import com.example.problemfinder.data.opportunities.impl.model.EvidenceSection
import com.example.problemfinder.data.opportunities.impl.model.entity.EvidenceDateCountEntity
import com.example.problemfinder.data.opportunities.impl.model.entity.EvidenceSourceCountEntity
import com.example.problemfinder.data.opportunities.impl.model.entity.OpportunityEntity
import com.example.problemfinder.data.opportunities.impl.model.entity.OpportunityEvidenceMembershipEntity
import com.example.problemfinder.data.opportunities.impl.util.toEntity
import java.util.UUID
import kotlin.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
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
class OpportunityDetailsLocalDataSourceTest {
    private lateinit var database: OpportunitiesDatabase
    private lateinit var dao: OpportunityDetailsDao
    private lateinit var local: OpportunityDetailsLocalDataSource

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder<OpportunitiesDatabase>(context())
            .setDriver(AndroidSQLiteDriver())
            .build()
        dao = database.discoveryDetailsDao()
        local = OpportunityDetailsLocalDataSource(database, dao)
    }

    @After
    fun tearDown() { database.close() }

    @Test
    fun missingParentCompletesCacheReadWithoutClaimingSync() = runBlocking {
        val result = snapshot()
        assertEquals(USER, result.userId)
        assertTrue(result.cacheReady)
        assertFalse(result.hasSynced)
        assertNull(result.details)
        assertTrue(result.posts.isEmpty())
        assertEquals(0, result.lastLoadedPage)
    }

    @Test
    fun firstPageReassemblesBothRolesOrderedCountsAndPreciseTimestamps() = runBlocking {
        val incoming = page()
        local.storePage(USER, incoming, replace = true)

        val result = snapshot()
        assertEquals(listOf(10).map(Int::toString), result.topEvidence.map { it.id })
        assertEquals(listOf(10, 20).map(Int::toString), result.posts.map { it.id })
        assertEquals(TIMESTAMP, result.details?.firstSeen)
        assertEquals(TIMESTAMP, result.posts.first().postedAt)
        assertEquals(listOf("github", "reddit"), result.details?.sourceBreakdown?.map { it.source })
        assertEquals(listOf("2026-10-21", "2026-10-22"), result.details?.postVolumeByDate?.map { it.date })
        assertEquals(1, result.lastLoadedPage)
        assertEquals(2, result.pageSize)
        assertEquals(6, result.totalPosts)
        assertTrue(result.hasSynced)
        // One content row can appear in two legitimate placements.
        val rows = dao.getDetailsRows(USER, CLUSTER)
        assertEquals(2, rows.count { it.evidence?.postId == "10" })
    }

    @Test
    fun successfullySyncedEmptyParentRemainsVisibleWithoutMemberships() = runBlocking {
        local.storePage(USER, page(postIds = emptyList(), topIds = emptyList(), totalPosts = 0,
            sources = emptyList(), dates = emptyList()), replace = true)
        val result = snapshot()
        assertEquals(CLUSTER, result.details?.id)
        assertTrue(result.topEvidence.isEmpty())
        assertTrue(result.posts.isEmpty())
        assertTrue(result.hasSynced)
        assertEquals(1, result.lastLoadedPage)
    }

    @Test
    fun appendUpdatesRepeatedContentWithoutMovingItsOrdinaryPlacement() = runBlocking {
        local.storePage(USER, page(), replace = true)
        local.storePage(USER, page(number = 2, postIds = listOf(20, 30), topIds = listOf(30),
            revision = "updated"), replace = false)

        val result = snapshot()
        assertEquals(listOf(10, 20, 30).map(Int::toString), result.posts.map { it.id })
        assertEquals("Post 20 updated", result.posts.single { it.id == "20" }.title)
        assertEquals(listOf(30).map(Int::toString), result.topEvidence.map { it.id })
        val repeated = dao.getDetailsRows(USER, CLUSTER).mapNotNull { it.membership }
            .single { it.section == EvidenceSection.POSTS && it.postId == "20" }
        assertEquals(1, repeated.page)
        assertEquals(1, repeated.position)
        assertEquals(2, result.lastLoadedPage)
    }

    @Test
    fun appendReplacesCompleteTopAndCountSetsIncludingEmptyResponses() = runBlocking {
        local.storePage(USER, page(topIds = listOf(10, 20)), replace = true)
        val newSources = listOf(EvidenceSourceCount("github", 6))
        val newDates = listOf(EvidenceDateCount("2026-10-23", 6))
        local.storePage(USER, page(number = 2, postIds = listOf(30, 40), topIds = listOf(30),
            sources = newSources, dates = newDates), replace = false)
        val second = snapshot()
        assertEquals(listOf(30).map(Int::toString), second.topEvidence.map { it.id })
        assertEquals(newSources, second.details?.sourceBreakdown)
        assertEquals(newDates, second.details?.postVolumeByDate)

        local.storePage(USER, page(number = 3, postIds = listOf(50, 60), topIds = emptyList(),
            sources = emptyList(), dates = emptyList()), replace = false)
        val third = snapshot()
        assertTrue(third.topEvidence.isEmpty())
        assertTrue(requireNotNull(third.details).sourceBreakdown.isEmpty())
        assertTrue(requireNotNull(third.details).postVolumeByDate.isEmpty())
        assertEquals(listOf(10, 20, 30, 40, 50, 60).map(Int::toString), third.posts.map { it.id })
    }

    @Test
    fun refreshResetsPagesWithoutDeletingTheSharedWatchState() = runBlocking {
        database.opportunityDao().upsertOpportunities(listOf(card(watched = true)))
        local.storePage(USER, page(), replace = true)
        local.storePage(USER, page(number = 2, postIds = listOf(30, 40)), replace = false)
        local.storePage(USER, page(postIds = listOf(50, 60), topIds = emptyList()), replace = true)
        val result = snapshot()
        assertEquals(listOf(50, 60).map(Int::toString), result.posts.map { it.id })
        assertEquals(1, result.lastLoadedPage)
        assertTrue(requireNotNull(result.cachedOpportunity).isWatched)
    }

    @Test
    fun invalidRequestsLeavePreviouslySavedDataAndProgressUnchanged() = runBlocking {
        val coldError = runCatching {
            local.storePage(USER, page(number = 2), replace = false)
        }.exceptionOrNull()
        assertTrue(coldError is IllegalArgumentException)
        assertNull(snapshot().details)

        local.storePage(USER, page(), replace = true)
        val before = snapshot()
        val invalidRequests = listOf(
            page(number = 1) to false,
            page(number = 3) to false,
            page(number = 2, pageSize = 3) to false,
            page(number = 2) to true,
        )
        for ((response, replace) in invalidRequests) {
            val error = runCatching { local.storePage(USER, response, replace) }.exceptionOrNull()
            assertTrue(error is IllegalArgumentException)
            assertEquals(before, snapshot())
        }
    }

    @Test
    fun appendRejectsAnUnsyncedParent() = runBlocking {
        dao.upsertOpportunityDetails(page().details.toEntity(USER, hasSynced = false,
            pageSize = 2, lastLoadedPage = 1))
        val before = snapshot()
        val error = runCatching {
            local.storePage(USER, page(number = 2), replace = false)
        }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
        assertEquals(before, snapshot())
    }

    @Test
    fun failedChildWriteRollsBackRefreshAndAppend() = runBlocking {
        local.storePage(USER, page(), replace = true)
        val before = snapshot()
        val failingDao = object : OpportunityDetailsDao by dao {
            override suspend fun upsertOpportunityEvidenceMembership(
                opportunityEvidenceMembership: List<OpportunityEvidenceMembershipEntity>,
            ) {
                dao.upsertOpportunityEvidenceMembership(opportunityEvidenceMembership)
                error("Injected membership failure")
            }
        }
        val failingLocal = OpportunityDetailsLocalDataSource(database, failingDao)
        for (replace in listOf(true, false)) {
            val response = page(number = if (replace) 1 else 2,
                postIds = listOf(30, 40), topIds = emptyList(), revision = "changed",
                sources = emptyList(), dates = emptyList())
            val error = runCatching { failingLocal.storePage(USER, response, replace) }.exceptionOrNull()
            assertTrue(error is IllegalStateException)
            assertEquals("Injected membership failure", error?.message)
            assertEquals(before, snapshot())
        }
    }

    @Test
    fun writesRemainScopedToTheOwningAccountAndCluster() = runBlocking {
        local.storePage(USER, page(), replace = true)
        local.storePage(OTHER_USER, page(revision = "other-account"), replace = true)
        local.storePage(USER, page(clusterId = 43, revision = "other-cluster"), replace = true)
        val otherAccount = snapshot(OTHER_USER)
        val otherCluster = snapshot(clusterId = 43)
        local.storePage(USER, page(number = 2, postIds = listOf(30), topIds = emptyList(),
            sources = emptyList(), dates = emptyList()), replace = false)
        local.storePage(USER, page(postIds = emptyList(), topIds = emptyList()), replace = true)
        assertEquals(otherAccount, snapshot(OTHER_USER))
        assertEquals(otherCluster, snapshot(clusterId = 43))
    }

    @Test
    fun countOnlyAndWatchOnlyWritesReemitCompleteSnapshots() = runBlocking {
        local.storePage(USER, page(), replace = true)
        database.opportunityDao().upsertOpportunities(listOf(card()))
        coroutineScope {
            val emissions = Channel<OpportunityDetailsSnapshot>(Channel.UNLIMITED)
            val collector = launch { local.observeDetails(USER, CLUSTER).collect { emissions.send(it) } }
            suspend fun next() = withTimeout(10_000) { emissions.receive() }
            try {
                val initial = next()
                assertFalse(requireNotNull(initial.cachedOpportunity).isWatched)
                database.withWriteTransaction {
                    dao.clearEvidenceSourceCount(USER, CLUSTER)
                    dao.upsertEvidenceSourceCount(listOf(EvidenceSourceCountEntity(USER, CLUSTER, "only-source", 6)))
                }
                val sourceChanged = next()
                assertEquals(listOf(EvidenceSourceCount("only-source", 6)), sourceChanged.details?.sourceBreakdown)
                assertEquals(initial.details?.postVolumeByDate, sourceChanged.details?.postVolumeByDate)

                database.withWriteTransaction {
                    dao.clearEvidenceDateCount(USER, CLUSTER)
                    dao.upsertEvidenceDateCount(listOf(EvidenceDateCountEntity(USER, CLUSTER, "2026-10-23", 6)))
                }
                val dateChanged = next()
                assertEquals(listOf(EvidenceDateCount("2026-10-23", 6)), dateChanged.details?.postVolumeByDate)
                assertEquals(sourceChanged.details?.sourceBreakdown, dateChanged.details?.sourceBreakdown)

                database.opportunityDao().setWatched(USER, CLUSTER, true)
                val watched = next()
                assertTrue(requireNotNull(watched.cachedOpportunity).isWatched)
                assertEquals(dateChanged.details, watched.details)
                assertEquals(initial.posts, watched.posts)
            } finally {
                collector.cancelAndJoin()
                emissions.close()
            }
        }
    }

    @Test
    fun concurrentRefreshCannotMixOldJoinedRowsWithNewCounts() = runBlocking {
        val name = "details-coherent-read-test-${UUID.randomUUID()}.db"
        val diskDatabase = openDiskDatabase(name)
        try {
            val diskDao = diskDatabase.discoveryDetailsDao()
            val diskLocal = OpportunityDetailsLocalDataSource(diskDatabase, diskDao)
            diskLocal.storePage(USER, page(), replace = true)
            val before = withTimeout(10_000) { diskLocal.observeDetails(USER, CLUSTER).first() }
            val rowsRead = CompletableDeferred<Unit>()
            val continueRead = CompletableDeferred<Unit>()
            val delayedDao = object : OpportunityDetailsDao by diskDao {
                override suspend fun getDetailsRows(userId: String, clusterId: Int) =
                    diskDao.getDetailsRows(userId, clusterId).also {
                        rowsRead.complete(Unit)
                        continueRead.await()
                    }
            }
            val delayedLocal = OpportunityDetailsLocalDataSource(diskDatabase, delayedDao)
            coroutineScope {
                val reader = async {
                    withTimeout(10_000) { delayedLocal.observeDetails(USER, CLUSTER).first() }
                }
                try {
                    withTimeout(10_000) { rowsRead.await() }
                    // Commit a refresh after joined rows were read but before count queries run.
                    // WAL allows this writer to finish while the reader retains its earlier snapshot.
                    withTimeout(10_000) {
                        diskLocal.storePage(USER, page(postIds = listOf(30, 40), topIds = listOf(30),
                            revision = "updated", sources = listOf(EvidenceSourceCount("new-source", 6)),
                            dates = listOf(EvidenceDateCount("2026-10-23", 6))), replace = true)
                    }
                } finally {
                    continueRead.complete(Unit)
                }
                assertEquals(before, reader.await())
            }
            val after = withTimeout(10_000) { diskLocal.observeDetails(USER, CLUSTER).first() }
            assertEquals(listOf(30, 40).map(Int::toString), after.posts.map { it.id })
            assertEquals(listOf(EvidenceSourceCount("new-source", 6)), after.details?.sourceBreakdown)
            assertEquals(listOf(EvidenceDateCount("2026-10-23", 6)), after.details?.postVolumeByDate)
        } finally {
            diskDatabase.close()
            context().deleteDatabase(name)
        }
    }

    @Test
    fun savedPageProgressAndEvidenceSurviveReopeningTheDatabase() = runBlocking {
        val name = "details-local-test-${UUID.randomUUID()}.db"
        var diskDatabase = openDiskDatabase(name)
        try {
            val firstLocal = OpportunityDetailsLocalDataSource(diskDatabase, diskDatabase.discoveryDetailsDao())
            firstLocal.storePage(USER, page(), replace = true)
            firstLocal.storePage(USER, page(number = 2, postIds = listOf(30, 40)), replace = false)
            diskDatabase.close()
            diskDatabase = openDiskDatabase(name)
            val reopened = OpportunityDetailsLocalDataSource(diskDatabase, diskDatabase.discoveryDetailsDao())
            val result = withTimeout(10_000) { reopened.observeDetails(USER, CLUSTER).first() }
            assertEquals(listOf(10, 20, 30, 40).map(Int::toString), result.posts.map { it.id })
            assertEquals(2, result.lastLoadedPage)
            assertEquals(2, result.pageSize)
            assertEquals(6, result.totalPosts)
            assertTrue(result.hasSynced)
        } finally {
            diskDatabase.close()
            context().deleteDatabase(name)
        }
    }

    private suspend fun snapshot(userId: String = USER, clusterId: Int = CLUSTER) =
        withTimeout(10_000) { local.observeDetails(userId, clusterId).first() }

    private fun context(): Context = ApplicationProvider.getApplicationContext()

    private fun openDiskDatabase(name: String) = Room.databaseBuilder<OpportunitiesDatabase>(context(), name)
        .setDriver(AndroidSQLiteDriver())
        .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
        .build()

    private fun card(watched: Boolean = false) = OpportunityEntity(
        userId = USER, clusterId = CLUSTER, name = "Cached card", summary = "Cached summary",
        problemStatement = "Cached problem", postCount = 6, category = "build_and_tooling",
        isWatched = watched, dateIso = null,
    )

    private fun page(
        number: Int = 1,
        pageSize: Int = 2,
        clusterId: Int = CLUSTER,
        postIds: List<Int> = listOf(10, 20),
        topIds: List<Int> = listOf(10),
        totalPosts: Int = 6,
        revision: String = "original",
        sources: List<EvidenceSourceCount> = listOf(EvidenceSourceCount("reddit", 4), EvidenceSourceCount("github", 2)),
        dates: List<EvidenceDateCount> = listOf(EvidenceDateCount("2026-10-22", 4), EvidenceDateCount("2026-10-21", 2)),
    ) = OpportunityDetailsPage(
        details = OpportunityDetails(
            id = clusterId, name = "Opportunity $clusterId $revision", summary = "Summary $revision",
            problemStatement = "Problem $revision", postCount = totalPosts, firstSeen = TIMESTAMP,
            lastSeen = TIMESTAMP, authorCount = 2, communityCount = 1,
            sourceBreakdown = sources, postVolumeByDate = dates,
        ),
        topEvidence = topIds.map { evidence(it, revision) },
        posts = postIds.map { evidence(it, revision) },
        page = number, pageSize = pageSize, totalPosts = totalPosts,
    )

    private fun evidence(id: Int, revision: String) = OpportunityEvidence(
        id = id.toString(), title = "Post $id $revision", body = "Body $revision", url = null,
        author = "author-$id", community = "community", source = "reddit", score = id,
        commentCount = 0, postedAt = TIMESTAMP, problemStatement = "Problem $id",
    )

    companion object {
        private const val USER = "details-user"
        private const val OTHER_USER = "other-user"
        private const val CLUSTER = 42
        private val TIMESTAMP = Instant.parse("2026-10-23T12:34:56.123456789Z")
    }
}
