package com.example.problemfinder.data.opportunities.impl.local

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.problemfinder.core.common.shared.OpportunityCategoryFilter
import com.example.problemfinder.core.common.shared.SortType
import com.example.problemfinder.core.model.browse.BrowsePage
import com.example.problemfinder.core.model.browse.BrowseSnapshot
import com.example.problemfinder.core.model.opportunity.Opportunity
import com.example.problemfinder.data.opportunities.impl.model.entity.BrowseQueryEntity
import com.example.problemfinder.data.opportunities.impl.model.entity.BrowseQueryItemEntity
import com.example.problemfinder.data.opportunities.impl.db.BrowseDao
import com.example.problemfinder.data.opportunities.impl.db.OpportunitiesDatabase
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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
import java.util.UUID
import kotlin.time.Instant

@RunWith(AndroidJUnit4::class)
class BrowseLocalDataSourceTest {
    private lateinit var context: Context
    private lateinit var database: OpportunitiesDatabase
    private lateinit var dao: BrowseDao
    private lateinit var source: BrowseLocalDataSource

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = createDatabase()
        dao = database.browseDao()
        source = BrowseLocalDataSource(database, dao, database.opportunityDao())
    }

    @After
    fun tearDown() {
        if (::database.isInitialized) database.close()
    }

    @Test
    fun uncachedQueryHasNotSynced() = runBlocking {
        assertEquals(BrowseSnapshot(), snapshot())
    }

    @Test
    fun successfulEmptyPageIsDifferentFromUncachedQuery() = runBlocking {
        source.replaceFirstPage(USER, OpportunityCategoryFilter.ALL, SortType.NEWEST, BrowsePage(emptyList()))

        assertEquals(BrowseSnapshot(hasSynced = true), snapshot())
    }

    @Test
    fun firstPagePreservesOrderNullsAndTimestampPrecisionAndRemovesDuplicates() = runBlocking {
        val first = cluster(31, watched = true, name = "Unclassified opportunity")
        val second = cluster(10, date = null)
        val duplicate = first.copy(name = "Duplicate should not replace first occurrence")
        source.replaceFirstPage(
            USER,
            OpportunityCategoryFilter.ALL,
            SortType.NEWEST,
            BrowsePage(listOf(first, second, duplicate), "opaque-cursor")
        )

        assertEquals(
            BrowseSnapshot(clusters = listOf(first, second), nextCursor = "opaque-cursor", hasSynced = true),
            snapshot()
        )
    }

    @Test
    fun refreshReplacesOldMembershipAndCursor() = runBlocking {
        source.replaceFirstPage(
            USER, OpportunityCategoryFilter.ALL, SortType.NEWEST,
            BrowsePage(listOf(cluster(10), cluster(20), cluster(30)), "old-cursor")
        )
        val refreshed = listOf(cluster(20, name = "Updated"), cluster(40))
        source.replaceFirstPage(
            USER, OpportunityCategoryFilter.ALL, SortType.NEWEST,
            BrowsePage(refreshed, "new-cursor")
        )

        assertEquals(BrowseSnapshot(clusters = refreshed, nextCursor = "new-cursor", hasSynced = true), snapshot())
    }

    @Test
    fun usersKeepSeparateContentWatchedStateAndCursorsForTheSameCluster() = runBlocking {
        val firstUserCluster = cluster(10, watched = true, name = "First user's cache")
        val secondUserCluster = cluster(10, watched = false, name = "Second user's cache")
        source.replaceFirstPage(
            USER, OpportunityCategoryFilter.ALL, SortType.NEWEST,
            BrowsePage(listOf(firstUserCluster), "first-user-cursor")
        )
        source.replaceFirstPage(
            OTHER_USER, OpportunityCategoryFilter.ALL, SortType.NEWEST,
            BrowsePage(listOf(secondUserCluster), "second-user-cursor")
        )

        assertEquals(
            BrowseSnapshot(clusters = listOf(firstUserCluster), nextCursor = "first-user-cursor", hasSynced = true),
            snapshot(USER)
        )
        assertEquals(
            BrowseSnapshot(clusters = listOf(secondUserCluster), nextCursor = "second-user-cursor", hasSynced = true),
            snapshot(OTHER_USER)
        )
    }

    @Test
    fun refreshingOneSortPreservesAnotherSortsMembershipAndCursor() = runBlocking {
        val largest = listOf(cluster(20))
        source.replaceFirstPage(
            USER, OpportunityCategoryFilter.ALL, SortType.NEWEST,
            BrowsePage(listOf(cluster(10)), "old-newest-cursor")
        )
        source.replaceFirstPage(
            USER, OpportunityCategoryFilter.ALL, SortType.LARGEST,
            BrowsePage(largest, "largest-cursor")
        )
        val newest = listOf(cluster(30))
        source.replaceFirstPage(
            USER, OpportunityCategoryFilter.ALL, SortType.NEWEST,
            BrowsePage(newest, "newest-cursor")
        )

        assertEquals(BrowseSnapshot(clusters = newest, nextCursor = "newest-cursor", hasSynced = true), snapshot())
        assertEquals(
            BrowseSnapshot(clusters = largest, nextCursor = "largest-cursor", hasSynced = true),
            snapshot(sort = SortType.LARGEST)
        )
    }

    @Test
    fun existingCollectorReceivesCompleteSnapshotsAfterCacheWrites() = runBlocking {
        val emissions = Channel<BrowseSnapshot>(Channel.UNLIMITED)
        val collector = launch {
            source.observeBrowse(USER, OpportunityCategoryFilter.ALL, SortType.NEWEST).collect {
                emissions.send(it)
            }
        }
        try {
            assertEquals(BrowseSnapshot(), withTimeout(10_000) { emissions.receive() })

            val clusters = listOf(cluster(30), cluster(10))
            source.replaceFirstPage(
                USER, OpportunityCategoryFilter.ALL, SortType.NEWEST,
                BrowsePage(clusters, "next-cursor")
            )
            assertEquals(
                BrowseSnapshot(clusters = clusters, nextCursor = "next-cursor", hasSynced = true),
                withTimeout(10_000) { emissions.receive() }
            )

            source.replaceFirstPage(USER, OpportunityCategoryFilter.ALL, SortType.NEWEST, BrowsePage(emptyList()))
            assertEquals(
                BrowseSnapshot(hasSynced = true),
                withTimeout(10_000) { emissions.receive() }
            )
        } finally {
            collector.cancelAndJoin()
            emissions.close()
        }
    }

    @Test
    fun failedMembershipWriteRollsBackClusterContentMembershipAndMetadata() = runBlocking {
        val original = listOf(cluster(10, name = "Original"), cluster(20))
        source.replaceFirstPage(
            USER, OpportunityCategoryFilter.ALL, SortType.NEWEST,
            BrowsePage(original, "original-cursor")
        )
        val before = snapshot()
        val failingDao = object : BrowseDao by dao {
            override suspend fun insertQueryItems(items: List<BrowseQueryItemEntity>) {
                dao.insertQueryItems(items)
                error("Injected membership write failure")
            }
        }
        val failingSource = BrowseLocalDataSource(database, failingDao, database.opportunityDao())

        val failure = runCatching {
            failingSource.replaceFirstPage(
                USER, OpportunityCategoryFilter.ALL, SortType.NEWEST,
                BrowsePage(listOf(cluster(10, name = "Changed"), cluster(99)), "changed-cursor")
            )
        }.exceptionOrNull()

        assertTrue(failure is IllegalStateException)
        assertEquals("Injected membership write failure", failure?.message)
        assertEquals(before, snapshot())
    }

    @Test
    fun closingAndReopeningTheDatabasePreservesCachedResults() = runBlocking {
        val name = "browse-cache-test-${UUID.randomUUID()}.db"
        val cached = listOf(cluster(20, watched = true), cluster(10, date = null))
        var fileDatabase: OpportunitiesDatabase? = null
        try {
            val firstDatabase = createDatabase(name)
            fileDatabase = firstDatabase
            BrowseLocalDataSource(firstDatabase, firstDatabase.browseDao(), firstDatabase.opportunityDao()).replaceFirstPage(
                USER, OpportunityCategoryFilter.ALL, SortType.NEWEST,
                BrowsePage(cached, "persisted-cursor")
            )
            firstDatabase.close()
            fileDatabase = null

            val reopenedDatabase = createDatabase(name)
            fileDatabase = reopenedDatabase
            val reopenedSource = BrowseLocalDataSource(reopenedDatabase, reopenedDatabase.browseDao(), reopenedDatabase.opportunityDao())
            val reopenedSnapshot = withTimeout(10_000) {
                reopenedSource.observeBrowse(USER, OpportunityCategoryFilter.ALL, SortType.NEWEST).first()
            }
            assertEquals(BrowseSnapshot(clusters = cached, nextCursor = "persisted-cursor", hasSynced = true), reopenedSnapshot)
        } finally {
            fileDatabase?.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun pageAppendUpdatesExistingContentWithoutMovingItAndAppendsDistinctNewIds() = runBlocking {
        val first = cluster(30)
        val existing = cluster(10)
        seed(BrowsePage(listOf(first, existing), "cursor-one"))
        val updated = existing.copy(name = "Updated", isWatched = true)
        val newCluster = cluster(20)

        assertTrue(append(
            "cursor-one",
            BrowsePage(
                listOf(updated, newCluster, newCluster.copy(name = "Ignored duplicate")),
                "cursor-two"
            )
        ))

        assertEquals(
            BrowseSnapshot(clusters = listOf(first, updated, newCluster), nextCursor = "cursor-two", hasSynced = true),
            snapshot()
        )
        assertEquals(
            listOf(30 to 0, 10 to 1, 20 to 2),
            dao.getQueryItems(USER, OpportunityCategoryFilter.ALL.name, SortType.NEWEST.name)
                .map { it.clusterId to it.position }
        )
    }

    @Test
    fun severalPagesPreserveOrderAndTheFinalPageClearsTheCursor() = runBlocking {
        val first = cluster(30)
        val second = cluster(20)
        val third = cluster(10)
        seed(BrowsePage(listOf(first), "cursor-one"))

        assertTrue(append("cursor-one", BrowsePage(listOf(second), "cursor-two")))
        assertTrue(append("cursor-two", BrowsePage(listOf(third))))

        assertEquals(BrowseSnapshot(clusters = listOf(first, second, third), nextCursor = null, hasSynced = true), snapshot())
        assertFalse(append("cursor-two", BrowsePage(listOf(cluster(99)))))
        assertEquals(BrowseSnapshot(clusters = listOf(first, second, third), nextCursor = null, hasSynced = true), snapshot())
    }

    @Test
    fun duplicateOnlyAndEmptyPagesStillAdvancePaginationMetadata() = runBlocking {
        val original = cluster(10)
        seed(BrowsePage(listOf(original), "cursor-one"))
        val updated = original.copy(name = "Updated duplicate")

        assertTrue(append("cursor-one", BrowsePage(listOf(updated), "cursor-two")))
        assertEquals(BrowseSnapshot(clusters = listOf(updated), nextCursor = "cursor-two", hasSynced = true), snapshot())
        assertTrue(append("cursor-two", BrowsePage(emptyList())))
        assertEquals(BrowseSnapshot(clusters = listOf(updated), nextCursor = null, hasSynced = true), snapshot())
    }

    @Test
    fun retryingAnAlreadyAppliedPageDoesNotOverwriteNewerCachedData() = runBlocking {
        seed(BrowsePage(listOf(cluster(10)), "cursor-one"))
        assertTrue(append("cursor-one", BrowsePage(listOf(cluster(20)), "cursor-two")))
        val before = snapshot()

        assertFalse(append(
            "cursor-one",
            BrowsePage(listOf(cluster(10, name = "Stale overwrite"), cluster(99)), "cursor-three")
        ))

        assertEquals(before, snapshot())
    }

    @Test
    fun refreshWithANewCursorRejectsAnOlderInFlightPage() = runBlocking {
        seed(BrowsePage(listOf(cluster(10)), "old-cursor"))
        val fresh = cluster(30, name = "Fresh")
        seed(BrowsePage(listOf(fresh), "fresh-cursor"))

        assertFalse(append(
            "old-cursor",
            BrowsePage(listOf(fresh.copy(name = "Stale"), cluster(99)), "old-cursor")
        ))

        assertEquals(BrowseSnapshot(clusters = listOf(fresh), nextCursor = "fresh-cursor", hasSynced = true), snapshot())
    }

    @Test
    fun aNonAdvancingCursorFailsWithoutChangingTheCache() = runBlocking {
        seed(BrowsePage(listOf(cluster(10)), "cursor-one"))
        val before = snapshot()

        val failure = runCatching {
            append("cursor-one", BrowsePage(listOf(cluster(20)), "cursor-one"))
        }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
        assertEquals("Browse cursor did not advance", failure?.message)
        assertEquals(before, snapshot())
    }

    @Test
    fun paginationBeforeFirstSyncDoesNotCreateCacheRecords() = runBlocking {
        assertFalse(append("cursor-one", BrowsePage(listOf(cluster(10)), "cursor-two")))

        assertEquals(BrowseSnapshot(), snapshot())
        assertNull(dao.getQuery(USER, OpportunityCategoryFilter.ALL.name, SortType.NEWEST.name))
    }

    @Test
    fun paginationRequiresASuccessfullySyncedQuery() = runBlocking {
        dao.upsertQuery(BrowseQueryEntity(
            userId = USER,
            category = OpportunityCategoryFilter.ALL.name,
            sort = SortType.NEWEST.name,
            nextCursor = "cursor-one",
            hasSynced = false
        ))
        val before = snapshot()

        assertFalse(append("cursor-one", BrowsePage(listOf(cluster(10)), "cursor-two")))

        assertEquals(before, snapshot())
    }

    @Test
    fun pageAppendUsesOnlyTheMatchingUsersQueryAndSort() = runBlocking {
        val first = cluster(10, watched = false)
        val secondUser = cluster(10, watched = true)
        val largest = cluster(30)
        seed(BrowsePage(listOf(first), "user-one-cursor"))
        seed(BrowsePage(listOf(secondUser), "user-two-cursor"), userId = OTHER_USER)
        seed(BrowsePage(listOf(largest), "largest-cursor"), sort = SortType.LARGEST)

        assertFalse(append("user-two-cursor", BrowsePage(listOf(cluster(99)), "wrong-cursor")))
        val next = cluster(20)
        assertTrue(append("user-one-cursor", BrowsePage(listOf(next), "user-one-next")))

        assertEquals(BrowseSnapshot(clusters = listOf(first, next), nextCursor = "user-one-next", hasSynced = true), snapshot())
        assertEquals(
            BrowseSnapshot(clusters = listOf(secondUser), nextCursor = "user-two-cursor", hasSynced = true),
            snapshot(OTHER_USER)
        )
        assertEquals(
            BrowseSnapshot(clusters = listOf(largest), nextCursor = "largest-cursor", hasSynced = true),
            snapshot(sort = SortType.LARGEST)
        )
    }

    @Test
    fun existingCollectorReceivesAppendedRowsAndCursorTogether() = runBlocking {
        val first = cluster(10)
        seed(BrowsePage(listOf(first), "cursor-one"))
        val emissions = Channel<BrowseSnapshot>(Channel.UNLIMITED)
        val collector = launch {
            source.observeBrowse(USER, OpportunityCategoryFilter.ALL, SortType.NEWEST).collect {
                emissions.send(it)
            }
        }
        try {
            assertEquals(
                BrowseSnapshot(clusters = listOf(first), nextCursor = "cursor-one", hasSynced = true),
                withTimeout(10_000) { emissions.receive() }
            )
            val updated = first.copy(name = "Updated")
            val next = cluster(20)
            assertTrue(append("cursor-one", BrowsePage(listOf(updated, next), "cursor-two")))
            assertEquals(
                BrowseSnapshot(clusters = listOf(updated, next), nextCursor = "cursor-two", hasSynced = true),
                withTimeout(10_000) { emissions.receive() }
            )
        } finally {
            collector.cancelAndJoin()
            emissions.close()
        }
    }

    @Test
    fun failedCursorWriteRollsBackAppendedRowsAndContentUpdates() = runBlocking {
        seed(BrowsePage(listOf(cluster(10, name = "Original")), "cursor-one"))
        val before = snapshot()
        val failingDao = object : BrowseDao by dao {
            override suspend fun upsertQuery(query: BrowseQueryEntity) {
                dao.upsertQuery(query)
                error("Injected cursor write failure")
            }
        }
        val failingSource = BrowseLocalDataSource(database, failingDao, database.opportunityDao())

        val failure = runCatching {
            failingSource.appendPage(
                USER, OpportunityCategoryFilter.ALL, SortType.NEWEST, "cursor-one",
                BrowsePage(listOf(cluster(10, name = "Changed"), cluster(20)), "cursor-two")
            )
        }.exceptionOrNull()

        assertTrue(failure is IllegalStateException)
        assertEquals("Injected cursor write failure", failure?.message)
        assertEquals(before, snapshot())
        assertEquals(
            listOf(10 to 0),
            dao.getQueryItems(USER, OpportunityCategoryFilter.ALL.name, SortType.NEWEST.name)
                .map { it.clusterId to it.position }
        )
    }

    @Test
    fun twoConcurrentResponsesForTheSameCursorApplyOnlyOnce() = runBlocking {
        val first = cluster(10)
        seed(BrowsePage(listOf(first), "cursor-one"))
        val pages = listOf(
            BrowsePage(listOf(cluster(20)), "cursor-two"),
            BrowsePage(listOf(cluster(30)), "cursor-three")
        )

        val applied = coroutineScope {
            pages.map { page -> async { append("cursor-one", page) } }.awaitAll()
        }

        assertEquals(1, applied.count { it })
        val winningPage = pages[applied.indexOf(true)]
        assertEquals(
            BrowseSnapshot(clusters = listOf(first) + winningPage.clusters, nextCursor = winningPage.nextCursor, hasSynced = true),
            snapshot()
        )
    }

    private suspend fun seed(
        page: BrowsePage,
        userId: String = USER,
        sort: SortType = SortType.NEWEST
    ) {
        source.replaceFirstPage(userId, OpportunityCategoryFilter.ALL, sort, page)
    }

    private suspend fun append(
        expectedCursor: String,
        page: BrowsePage,
        userId: String = USER,
        sort: SortType = SortType.NEWEST
    ): Boolean = source.appendPage(userId, OpportunityCategoryFilter.ALL, sort, expectedCursor, page)

    private fun createDatabase(name: String? = null): OpportunitiesDatabase {
        val builder = if (name == null) {
            Room.inMemoryDatabaseBuilder<OpportunitiesDatabase>(context)
        } else {
            Room.databaseBuilder<OpportunitiesDatabase>(context, name)
        }
        return builder.setDriver(AndroidSQLiteDriver()).build()
    }

    private suspend fun snapshot(
        userId: String = USER,
        sort: SortType = SortType.NEWEST
    ): BrowseSnapshot = withTimeout(10_000) {
        source.observeBrowse(userId, OpportunityCategoryFilter.ALL, sort).first()
    }

    private fun cluster(
        id: Int,
        watched: Boolean = false,
        name: String = "Cluster $id",
        date: Instant? = Instant.parse("2026-10-10T12:00:00.123456789Z")
    ) = Opportunity(
        id = id,
        name = name,
        summary = "A complete opportunity summary for the preview.",
        problemStatement = "Teams need a reliable way to complete this task.",
        postCount = id,
        isWatched = watched,
        date = date
    )

    private companion object {
        const val USER = "user-one"
        const val OTHER_USER = "user-two"
    }
}
