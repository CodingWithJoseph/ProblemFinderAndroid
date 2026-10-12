package com.example.problemfinder.data.browse.impl

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.problemfinder.core.common.shared.Category
import com.example.problemfinder.core.common.shared.SortType
import com.example.problemfinder.core.model.browse.BrowseFailure
import com.example.problemfinder.core.model.browse.BrowseOperationException
import com.example.problemfinder.core.model.browse.BrowsePage
import com.example.problemfinder.core.model.browse.BrowseSnapshot
import com.example.problemfinder.core.model.discovery.DiscoveryCluster
import com.example.problemfinder.data.authentication.api.AuthSession
import com.example.problemfinder.data.authentication.api.AuthSessionProvider
import com.example.problemfinder.data.browse.impl.local.BrowseDao
import com.example.problemfinder.data.browse.impl.local.BrowseDatabase
import com.example.problemfinder.data.browse.impl.local.BrowseLocalDataSource
import com.example.problemfinder.data.browse.impl.model.BrowseApiConfig
import com.example.problemfinder.data.browse.impl.model.dto.BrowsePageDto
import com.example.problemfinder.data.browse.impl.model.dto.DiscoveryClusterDto
import com.example.problemfinder.data.browse.impl.model.entity.BrowseQueryEntity
import com.example.problemfinder.data.browse.impl.remote.BrowseRemoteDataSource
import com.example.problemfinder.data.browse.impl.remote.createBrowseHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.io.IOException
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Instant

@RunWith(AndroidJUnit4::class)
class BrowseRepositoryTest {
    private lateinit var database: BrowseDatabase
    private lateinit var dao: BrowseDao
    private lateinit var local: BrowseLocalDataSource
    private lateinit var client: HttpClient
    private lateinit var sessions: FakeSessions
    private lateinit var repository: BrowseRepositoryImpl
    private val requests = CopyOnWriteArrayList<HttpRequestData>()
    private val metadataReads = Channel<BrowseQueryEntity?>(Channel.UNLIMITED)
    private var handler: MockRequestHandler = {
        respond(pageJson(listOf(1), "cursor-1"), HttpStatusCode.OK, JSON_HEADERS)
    }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder<BrowseDatabase>(context)
            .setDriver(AndroidSQLiteDriver())
            .build()
        dao = database.browseDao()
        val trackedDao = object : BrowseDao by dao {
            override suspend fun getQuery(
                userId: String,
                category: String,
                sort: String,
            ): BrowseQueryEntity? {
                val result = dao.getQuery(userId, category, sort)
                metadataReads.send(result)
                return result
            }
        }
        local = BrowseLocalDataSource(database, trackedDao)
        sessions = FakeSessions()
        client = createBrowseHttpClient(MockEngine) {
            addHandler { request ->
                requests.add(request)
                handler(request)
            }
        }
        repository = BrowseRepositoryImpl(
            sessions,
            BrowseRemoteDataSource(client, BrowseApiConfig("https://browse.example")),
            local,
        )
    }

    @After
    fun tearDown() {
        if (::client.isInitialized) client.close()
        if (::database.isInitialized) database.close()
        metadataReads.close()
    }

    @Test
    fun observationReadsExistingCacheWithoutCredentialsOrNetwork() = scenario {
        seed(listOf(8, 3), "cached-cursor")
        sessions.session = null

        val observed = repository.observeBrowse(Category.ALL, SortType.NEWEST)
            .first { it.hasSynced }

        assertEquals(cachedSnapshot(listOf(8, 3), "cached-cursor").copy(userId = USER), observed)
        assertTrue(requests.isEmpty())
        assertTrue(sessions.sessionCalls.tryReceive().isFailure)
    }

    @Test
    fun observationSwitchesAccountsAndClearsOnSignOut() = scenario {
        local.replaceFirstPage(
            USER, Category.ALL, SortType.NEWEST,
            BrowsePage(listOf(cluster(1, "First user", watched = true)), "user-a"),
        )
        local.replaceFirstPage(
            OTHER_USER, Category.ALL, SortType.NEWEST,
            BrowsePage(listOf(cluster(1, "Second user")), "user-b"),
        )
        coroutineScope {
            val emissions = Channel<BrowseSnapshot>(Channel.UNLIMITED)
            val collector = launch {
                repository.observeBrowse(Category.ALL, SortType.NEWEST)
                    .collect { emissions.send(it) }
            }
            try {
                assertEquals(BrowseSnapshot(userId = USER, cacheReady = false), emissions.receive())
                assertEquals("First user 1", emissions.receive().clusters.single().name)

                sessions.switchTo(OTHER_USER)
                assertEquals(BrowseSnapshot(userId = OTHER_USER, cacheReady = false), emissions.receive())
                val secondUser = emissions.receive()
                assertEquals(OTHER_USER, secondUser.userId)
                assertTrue(secondUser.cacheReady)
                assertEquals("Second user 1", secondUser.clusters.single().name)
                assertFalse(secondUser.clusters.single().isWatched)
                assertEquals("user-b", secondUser.nextCursor)

                sessions.switchTo(null)
                assertEquals(BrowseSnapshot(), emissions.receive())
                assertTrue(requests.isEmpty())
                assertEquals("user-a", snapshot(USER).nextCursor)
            } finally {
                collector.cancelAndJoin()
                emissions.close()
            }
        }
    }

    @Test
    fun refreshWritesAuthenticatedResponseAndUpdatesAnExistingObserver() = scenario {
        seed(listOf(8, 3), "old-cursor")
        coroutineScope {
            val emissions = Channel<BrowseSnapshot>(Channel.UNLIMITED)
            val collector = launch {
                repository.observeBrowse(Category.ALL, SortType.NEWEST)
                    .collect { emissions.send(it) }
            }
            try {
                assertEquals(BrowseSnapshot(userId = USER, cacheReady = false), emissions.receive())
                assertEquals(cachedSnapshot(listOf(8, 3), "old-cursor").copy(userId = USER), emissions.receive())

                repository.refresh(Category.ALL, SortType.NEWEST)

                assertEquals(cachedSnapshot(listOf(1), "cursor-1").copy(userId = USER), emissions.receive())
                val request = requests.single()
                assertEquals("Bearer test-token-$USER", request.headers[HttpHeaders.Authorization])
                assertEquals("all", request.url.parameters["category"])
                assertEquals("newest", request.url.parameters["sort"])
                assertFalse(request.url.parameters.contains("cursor"))
            } finally {
                collector.cancelAndJoin()
                emissions.close()
            }
        }
    }

    @Test
    fun observationDistinguishesCacheReadInProgressFromAnUncachedAccount() = scenario {
        val emissions = repository.observeBrowse(Category.ALL, SortType.NEWEST).take(2).toList()

        assertEquals(BrowseSnapshot(userId = USER, cacheReady = false), emissions[0])
        assertEquals(BrowseSnapshot(userId = USER), emissions[1])
        assertTrue(requests.isEmpty())
    }

    @Test
    fun refreshRemovesPreviouslyLoadedPagesAndSuccessfulEmptyResponseIsSynced() = scenario {
        seed(listOf(1), "cursor-1")
        local.appendPage(
            USER, Category.ALL, SortType.NEWEST, "cursor-1",
            BrowsePage(listOf(cluster(2), cluster(3)), "cursor-2"),
        )
        handler = { respond(pageJson(), HttpStatusCode.OK, JSON_HEADERS) }

        repository.refresh(Category.ALL, SortType.NEWEST)

        assertEquals(BrowseSnapshot(hasSynced = true), snapshot())
    }

    @Test
    fun repeatedPaginationUsesStoredCursorsUpdatesDuplicatesAndStopsAtEnd() = scenario {
        seed(listOf(1), "opaque+/= cursor?")
        handler = { request ->
            val body = when (request.url.parameters["cursor"]) {
                "opaque+/= cursor?" -> pageJson(listOf(1, 2, 2), "cursor-2", "Updated")
                "cursor-2" -> pageJson(listOf(3))
                else -> error("Unexpected cursor")
            }
            respond(body, HttpStatusCode.OK, JSON_HEADERS)
        }

        repository.loadNextPage(Category.ALL, SortType.NEWEST)
        repository.loadNextPage(Category.ALL, SortType.NEWEST)
        repository.loadNextPage(Category.ALL, SortType.NEWEST)

        assertEquals(listOf(1, 2, 3), snapshot().clusters.map { it.id })
        assertEquals("Updated 1", snapshot().clusters.first().name)
        assertNull(snapshot().nextCursor)
        assertEquals(listOf("opaque+/= cursor?", "cursor-2"), requestCursors())
    }

    @Test
    fun missingUnsyncedAndCompletedQueriesDoNotRequestAnotherPage() = scenario {
        repository.loadNextPage(Category.ALL, SortType.NEWEST)
        dao.upsertQuery(BrowseQueryEntity(USER, "ALL", "NEWEST", "cursor-1", false))
        repository.loadNextPage(Category.ALL, SortType.NEWEST)
        seed(listOf(1), null)
        repository.loadNextPage(Category.ALL, SortType.NEWEST)

        assertTrue(requests.isEmpty())
    }

    @Test
    fun paginationUsesTheSelectedUsersAndSortsCursorOnly() = scenario {
        seed(listOf(1), "newest-cursor")
        local.replaceFirstPage(
            USER, Category.ALL, SortType.OLDEST,
            BrowsePage(listOf(cluster(2)), "oldest-cursor"),
        )
        local.replaceFirstPage(
            OTHER_USER, Category.ALL, SortType.OLDEST,
            BrowsePage(listOf(cluster(4)), "other-user-cursor"),
        )
        handler = { respond(pageJson(listOf(3)), HttpStatusCode.OK, JSON_HEADERS) }

        repository.loadNextPage(Category.ALL, SortType.OLDEST)

        assertEquals("oldest", requests.single().url.parameters["sort"])
        assertEquals("oldest-cursor", requests.single().url.parameters["cursor"])
        assertEquals(listOf(2, 3), snapshot(sort = SortType.OLDEST).clusters.map { it.id })
        assertEquals("newest-cursor", snapshot().nextCursor)
        assertEquals("other-user-cursor", snapshot(OTHER_USER, SortType.OLDEST).nextCursor)
    }

    @Test
    fun concurrentPageCallsThatCapturedTheSameCursorMakeOneRequest() = scenario {
        seed(listOf(1), "cursor-1")
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        handler = {
            entered.complete(Unit)
            release.await()
            respond(pageJson(listOf(2), "cursor-2"), HttpStatusCode.OK, JSON_HEADERS)
        }
        coroutineScope {
            val first = async { repository.loadNextPage(Category.ALL, SortType.NEWEST) }
            entered.await()
            // The first call has read metadata before and after locking.
            repeat(2) { assertEquals("cursor-1", metadataReads.receive()?.nextCursor) }
            val second = async { repository.loadNextPage(Category.ALL, SortType.NEWEST) }
            // Prove the second call captured the old cursor before releasing the first.
            assertEquals("cursor-1", metadataReads.receive()?.nextCursor)
            release.complete(Unit)
            first.await()
            second.await()
        }

        assertEquals(listOf("cursor-1"), requestCursors())
        assertEquals(cachedSnapshot(listOf(1, 2), "cursor-2"), snapshot())
    }

    @Test
    fun refreshWaitsForInFlightPageThenReplacesItsMembership() = scenario {
        seed(listOf(1), "cursor-1")
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        handler = { request ->
            if (request.url.parameters.contains("cursor")) {
                entered.complete(Unit)
                release.await()
                respond(pageJson(listOf(2), "cursor-2"), HttpStatusCode.OK, JSON_HEADERS)
            } else {
                respond(pageJson(listOf(9), "refreshed-cursor"), HttpStatusCode.OK, JSON_HEADERS)
            }
        }
        coroutineScope {
            val page = async { repository.loadNextPage(Category.ALL, SortType.NEWEST) }
            entered.await()
            sessions.sessionCalls.receive()
            val refresh = async { repository.refresh(Category.ALL, SortType.NEWEST) }
            sessions.sessionCalls.receive()
            assertEquals(1, requests.size)
            release.complete(Unit)
            page.await()
            refresh.await()
        }

        assertEquals(listOf("cursor-1", null), requestCursors())
        assertEquals(cachedSnapshot(listOf(9), "refreshed-cursor"), snapshot())
    }

    @Test
    fun pageWaitingForRefreshDoesNotUseTheReplacedCursor() = scenario {
        seed(listOf(1), "old-cursor")
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        handler = {
            entered.complete(Unit)
            release.await()
            respond(pageJson(listOf(9), "new-cursor"), HttpStatusCode.OK, JSON_HEADERS)
        }
        coroutineScope {
            val refresh = async { repository.refresh(Category.ALL, SortType.NEWEST) }
            entered.await()
            val page = async { repository.loadNextPage(Category.ALL, SortType.NEWEST) }
            assertEquals("old-cursor", metadataReads.receive()?.nextCursor)
            release.complete(Unit)
            refresh.await()
            page.await()
        }

        assertEquals(1, requests.size)
        assertEquals(cachedSnapshot(listOf(9), "new-cursor"), snapshot())
    }

    @Test
    fun httpFailuresAreClassifiedAndPreserveTheEntireCachedSnapshot() = scenario {
        seed(listOf(8, 3), "cached-cursor")
        val before = snapshot()
        val cases = listOf(
            401 to BrowseFailure.SESSION_UNAVAILABLE,
            403 to BrowseFailure.REQUEST_REJECTED,
            422 to BrowseFailure.REQUEST_REJECTED,
            500 to BrowseFailure.SERVER,
            503 to BrowseFailure.SERVER,
        )
        for ((status, expected) in cases) {
            handler = { respond("{\"detail\":\"failure\"}", HttpStatusCode.fromValue(status), JSON_HEADERS) }
            expectFailure(expected) { repository.refresh(Category.ALL, SortType.NEWEST) }
            assertEquals("HTTP $status changed the cache", before, snapshot())
        }
    }

    @Test
    fun transportFailurePreservesCursorAndAPageRetrySucceeds() = scenario {
        seed(listOf(1), "cursor-1")
        handler = { throw IOException("Connection unavailable") }
        expectFailure(BrowseFailure.NETWORK) {
            repository.loadNextPage(Category.ALL, SortType.NEWEST)
        }
        assertEquals(cachedSnapshot(listOf(1), "cursor-1"), snapshot())

        handler = { respond(pageJson(listOf(2)), HttpStatusCode.OK, JSON_HEADERS) }
        repository.loadNextPage(Category.ALL, SortType.NEWEST)

        assertEquals(listOf("cursor-1", "cursor-1"), requestCursors())
        assertEquals(cachedSnapshot(listOf(1, 2), null), snapshot())
    }

    @Test
    fun malformedJsonAndInvalidDatesDoNotOverwriteCache() = scenario {
        seed(listOf(1), "cursor-1")
        val before = snapshot()
        for (body in listOf("{broken", pageJson(listOf(2), date = "not-a-date"))) {
            handler = { respond(body, HttpStatusCode.OK, JSON_HEADERS) }
            expectFailure(BrowseFailure.INVALID_RESPONSE) {
                repository.refresh(Category.ALL, SortType.NEWEST)
            }
            assertEquals(before, snapshot())
        }
    }

    @Test
    fun nonAdvancingCursorIsAnInvalidResponseAndDoesNotChangeCache() = scenario {
        seed(listOf(1), "cursor-1")
        handler = { respond(pageJson(listOf(2), "cursor-1"), HttpStatusCode.OK, JSON_HEADERS) }

        expectFailure(BrowseFailure.INVALID_RESPONSE) {
            repository.loadNextPage(Category.ALL, SortType.NEWEST)
        }

        assertEquals(cachedSnapshot(listOf(1), "cursor-1"), snapshot())
    }

    @Test
    fun unavailableCredentialsDoNotMakeRequestsOrDeleteTheCache() = scenario {
        seed(listOf(1), "cursor-1")
        sessions.session = null

        expectFailure(BrowseFailure.SESSION_UNAVAILABLE) {
            repository.refresh(Category.ALL, SortType.NEWEST)
        }
        expectFailure(BrowseFailure.SESSION_UNAVAILABLE) {
            repository.loadNextPage(Category.ALL, SortType.NEWEST)
        }

        assertTrue(requests.isEmpty())
        assertEquals(cachedSnapshot(listOf(1), "cursor-1"), snapshot())
    }

    @Test
    fun accountSwitchDiscardsInFlightResponseAndQueuedOldAccountRequest() = scenario {
        seed(listOf(1), "user-a-cursor")
        seed(listOf(4), "user-b-cursor", OTHER_USER)
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        handler = {
            entered.complete(Unit)
            release.await()
            respond(pageJson(listOf(9)), HttpStatusCode.OK, JSON_HEADERS)
        }
        coroutineScope {
            val first = async { repository.refresh(Category.ALL, SortType.NEWEST) }
            entered.await()
            sessions.sessionCalls.receive()
            val queued = async { repository.refresh(Category.ALL, SortType.NEWEST) }
            sessions.sessionCalls.receive()
            sessions.switchTo(OTHER_USER)
            release.complete(Unit)
            expectCancellation { first.await() }
            expectCancellation { queued.await() }
        }

        assertEquals(1, requests.size)
        assertEquals(cachedSnapshot(listOf(1), "user-a-cursor"), snapshot(USER))
        assertEquals(cachedSnapshot(listOf(4), "user-b-cursor"), snapshot(OTHER_USER))

        handler = { respond(pageJson(listOf(7)), HttpStatusCode.OK, JSON_HEADERS) }
        repository.refresh(Category.ALL, SortType.NEWEST)
        assertEquals("Bearer test-token-$OTHER_USER", requests.last().headers[HttpHeaders.Authorization])
        assertEquals(cachedSnapshot(listOf(7), null), snapshot(OTHER_USER))
        assertEquals(cachedSnapshot(listOf(1), "user-a-cursor"), snapshot(USER))
    }

    @Test
    fun signOutDuringPageRequestDiscardsTheResponse() = scenario {
        seed(listOf(1), "cursor-1")
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        handler = {
            entered.complete(Unit)
            release.await()
            respond(pageJson(listOf(2)), HttpStatusCode.OK, JSON_HEADERS)
        }
        coroutineScope {
            val page = async { repository.loadNextPage(Category.ALL, SortType.NEWEST) }
            entered.await()
            sessions.switchTo(null)
            release.complete(Unit)
            expectCancellation { page.await() }
        }

        assertEquals(cachedSnapshot(listOf(1), "cursor-1"), snapshot())
        assertEquals(BrowseSnapshot(), repository.observeBrowse(Category.ALL, SortType.NEWEST).first())
    }

    @Test
    fun cancellingAnInFlightRequestPropagatesAndReleasesTheMutex() = scenario {
        seed(listOf(1), "cursor-1")
        val entered = CompletableDeferred<Unit>()
        handler = {
            entered.complete(Unit)
            CompletableDeferred<Unit>().await()
            error("Cancelled request must not return")
        }
        coroutineScope {
            val refresh = async { repository.refresh(Category.ALL, SortType.NEWEST) }
            entered.await()
            refresh.cancelAndJoin()
            expectCancellation { refresh.await() }
        }
        assertEquals(cachedSnapshot(listOf(1), "cursor-1"), snapshot())

        handler = { respond(pageJson(listOf(7)), HttpStatusCode.OK, JSON_HEADERS) }
        repository.refresh(Category.ALL, SortType.NEWEST)
        assertEquals(cachedSnapshot(listOf(7), null), snapshot())
    }

    @Test
    fun cancellingARequestWaitingForTheMutexDoesNotSendItLater() = scenario {
        seed(listOf(1), "cursor-1")
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        handler = {
            entered.complete(Unit)
            release.await()
            respond(pageJson(listOf(2)), HttpStatusCode.OK, JSON_HEADERS)
        }
        coroutineScope {
            val page = async { repository.loadNextPage(Category.ALL, SortType.NEWEST) }
            entered.await()
            sessions.sessionCalls.receive()
            val queuedRefresh = async { repository.refresh(Category.ALL, SortType.NEWEST) }
            sessions.sessionCalls.receive()
            queuedRefresh.cancelAndJoin()
            expectCancellation { queuedRefresh.await() }
            release.complete(Unit)
            page.await()
        }

        assertEquals(1, requests.size)
        assertEquals(cachedSnapshot(listOf(1, 2), null), snapshot())
    }

    @Test
    fun tokenRotationForTheSameAccountDoesNotDiscardASuccessfulResponse() = scenario {
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        handler = {
            entered.complete(Unit)
            release.await()
            respond(pageJson(listOf(1)), HttpStatusCode.OK, JSON_HEADERS)
        }
        coroutineScope {
            val refresh = async { repository.refresh(Category.ALL, SortType.NEWEST) }
            entered.await()
            sessions.session = AuthSession(USER, "rotated-token")
            release.complete(Unit)
            refresh.await()
        }
        assertEquals(cachedSnapshot(listOf(1), null), snapshot())

        repository.refresh(Category.ALL, SortType.NEWEST)
        assertEquals("Bearer rotated-token", requests.last().headers[HttpHeaders.Authorization])
    }

    private fun scenario(block: suspend () -> Unit) = runBlocking {
        withTimeout(15_000) { block() }
    }

    private suspend fun seed(ids: List<Int>, cursor: String?, user: String = USER) {
        local.replaceFirstPage(
            user, Category.ALL, SortType.NEWEST, BrowsePage(ids.map { cluster(it) }, cursor),
        )
    }

    private suspend fun snapshot(user: String = USER, sort: SortType = SortType.NEWEST) =
        local.observeBrowse(user, Category.ALL, sort).first()

    private fun requestCursors() = requests.map { it.url.parameters["cursor"] }

    private suspend fun expectFailure(expected: BrowseFailure, block: suspend () -> Unit) {
        try {
            block()
            fail("Expected $expected")
        } catch (error: BrowseOperationException) {
            assertEquals(expected, error.failure)
        }
    }

    private suspend fun expectCancellation(block: suspend () -> Unit) {
        try {
            block()
            fail("Expected cancellation")
        } catch (_: CancellationException) {
            // Cancellation must reach the caller rather than become a Browse failure.
        }
    }

    private class FakeSessions : AuthSessionProvider {
        private val user = MutableStateFlow<String?>(USER)
        var session: AuthSession? = AuthSession(USER, "test-token-$USER")
        val sessionCalls = Channel<Unit>(Channel.UNLIMITED)

        override fun observeUserId(): Flow<String?> = user

        override suspend fun currentSession(): AuthSession? {
            val captured = session
            sessionCalls.send(Unit)
            return captured
        }

        fun switchTo(userId: String?) {
            session = userId?.let { AuthSession(it, "test-token-$it") }
            user.value = userId
        }
    }

    companion object {
        private const val USER = "user-a"
        private const val OTHER_USER = "user-b"
        private const val DATE = "2026-10-10T12:00:00.123456789Z"
        private val JSON_HEADERS = headersOf(HttpHeaders.ContentType, "application/json")

        private fun cluster(id: Int, prefix: String = "Opportunity", watched: Boolean = false) =
            DiscoveryCluster(
                id = id,
                name = "$prefix $id",
                summary = null,
                problemStatement = null,
                postCount = id,
                isWatched = watched,
                date = Instant.parse(DATE),
            )

        private fun cachedSnapshot(ids: List<Int>, cursor: String?) =
            BrowseSnapshot(clusters = ids.map { cluster(it) }, nextCursor = cursor, hasSynced = true)

        private fun pageJson(
            ids: List<Int> = emptyList(),
            cursor: String? = null,
            prefix: String = "Opportunity",
            date: String? = DATE,
        ): String = Json.encodeToString(
            BrowsePageDto(
                clusters = ids.map { id ->
                    DiscoveryClusterDto(id, "$prefix $id", null, null, id, false, date)
                },
                nextCursor = cursor,
            ),
        )
    }
}
