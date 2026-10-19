package com.example.problemfinder.data.opportunities.impl

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.problemfinder.core.common.shared.OpportunityCategoryFilter
import com.example.problemfinder.core.common.shared.SortType
import com.example.problemfinder.core.model.browse.BrowsePage
import com.example.problemfinder.core.model.discovery.DiscoveryCollection
import com.example.problemfinder.core.model.discovery.DiscoveryDashboard
import com.example.problemfinder.core.model.opportunity.Opportunity
import com.example.problemfinder.core.model.opportunity.OpportunityCategory
import com.example.problemfinder.core.model.opportunity.OpportunityFailure
import com.example.problemfinder.core.model.opportunity.OpportunityOperationException
import com.example.problemfinder.data.authentication.api.AuthSession
import com.example.problemfinder.data.authentication.api.AuthSessionProvider
import com.example.problemfinder.data.opportunities.impl.db.OpportunitiesDatabase
import com.example.problemfinder.data.opportunities.impl.local.BrowseLocalDataSource
import com.example.problemfinder.data.opportunities.impl.local.DiscoveryLocalDataSource
import com.example.problemfinder.data.opportunities.impl.model.OpportunitiesApiConfig
import com.example.problemfinder.data.opportunities.impl.remote.OpportunityActionsRemoteDataSource
import com.example.problemfinder.data.opportunities.impl.remote.BrowseRemoteDataSource
import com.example.problemfinder.data.opportunities.impl.remote.createOpportunitiesHttpClient
import com.example.problemfinder.data.opportunities.impl.util.OpportunitySyncCoordinator
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.http.content.TextContent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class OpportunityActionsRepositoryTest {
    private lateinit var db: OpportunitiesDatabase
    private lateinit var browse: BrowseLocalDataSource
    private lateinit var discovery: DiscoveryLocalDataSource
    private val sessions = Sessions()
    private val sync = OpportunitySyncCoordinator()
    private val requests = mutableListOf<HttpRequestData>()
    private var handler: MockRequestHandler = { request ->
        respond(if (request.method == HttpMethod.Post) """{"is_watched":true,"pipeline_id":"project-1"}""" else """{"is_watched":false}""", HttpStatusCode.OK, headers)
    }
    private val client = createOpportunitiesHttpClient(MockEngine) { addHandler { request -> requests += request; handler(request) } }
    private lateinit var actions: OpportunityActionsRepositoryImpl

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder<OpportunitiesDatabase>(ApplicationProvider.getApplicationContext<Context>()).setDriver(AndroidSQLiteDriver()).build()
        browse = BrowseLocalDataSource(db, db.browseDao(), db.opportunityDao())
        discovery = DiscoveryLocalDataSource(db, db.opportunityDao(), db.discoveryDao())
        actions = OpportunityActionsRepositoryImpl(sessions, OpportunityActionsRemoteDataSource(client, config), db.opportunityDao(), sync)
    }
    @After fun teardown() { client.close(); db.close() }

    @Test fun addAndRemoveUseExistingApiAndUpdateEverySharedOccurrence() = scenario {
        seed()
        seed("other")
        actions.setWatched("user", 42, true)
        val add = requests.single()
        assertEquals(HttpMethod.Post, add.method)
        assertEquals("/pipeline/watch", add.url.encodedPath)
        assertEquals("Bearer test-user-token", add.headers[HttpHeaders.Authorization])
        assertEquals("""{"cluster_id":"42"}""", (add.body as TextContent).text)
        assertMembership(true)
        assertFalse(browse.observeBrowse("other", OpportunityCategoryFilter.ALL, SortType.NEWEST).first().clusters.single().isWatched)
        actions.setWatched("user", 42, false)
        assertEquals(HttpMethod.Delete, requests.last().method)
        assertEquals("/pipeline/watch/42", requests.last().url.encodedPath)
        assertMembership(false)
        actions.setWatched("user", 42, true)
        assertMembership(true)
        assertEquals(3, requests.size)
    }

    @Test fun failuresAndUnexpectedMembershipLeaveCachedStateUntouched() = scenario {
        seed()
        val failures = listOf<MockRequestHandler>(
            { throw IOException("Offline") },
            { respond("{}", HttpStatusCode.Unauthorized, headers) },
            { respond("{}", HttpStatusCode.ServiceUnavailable, headers) },
            { respond("""{"is_watched":false}""", HttpStatusCode.OK, headers) },
            { respond("{}", HttpStatusCode.OK, headers) },
        )
        val expected = listOf(OpportunityFailure.NETWORK, OpportunityFailure.SESSION_UNAVAILABLE, OpportunityFailure.SERVER, OpportunityFailure.INVALID_RESPONSE, OpportunityFailure.INVALID_RESPONSE)
        failures.zip(expected).forEach { (next, failure) ->
            handler = next
            try { actions.setWatched("user", 42, true); fail("Expected failure") }
            catch (error: OpportunityOperationException) { assertEquals(failure, error.failure) }
            assertMembership(false)
        }
    }

    @Test fun missingSessionDoesNotSendARequest() = scenario {
        seed()
        sessions.session = null
        try { actions.setWatched("user", 42, true); fail("Expected unavailable session") }
        catch (error: OpportunityOperationException) { assertEquals(OpportunityFailure.SESSION_UNAVAILABLE, error.failure) }
        assertTrue(requests.isEmpty())
        assertMembership(false)
    }

    @Test fun staleScreenAccountCannotMutateTheNewAccount() = scenario {
        seed()
        sessions.change("other")
        try { actions.setWatched("user", 42, true); fail("Expected cancellation") } catch (_: CancellationException) {}
        assertTrue(requests.isEmpty())
        assertMembership(false)
    }

    @Test fun accountSwitchDuringRequestCannotUpdateEitherAccountCache() = scenario {
        seed(); seed("other")
        handler = { sessions.change("other"); respond("""{"is_watched":true}""", HttpStatusCode.OK, headers) }
        try { actions.setWatched("user", 42, true); fail("Expected cancellation") } catch (_: CancellationException) {}
        assertMembership(false)
        assertFalse(browse.observeBrowse("other", OpportunityCategoryFilter.ALL, SortType.NEWEST).first().clusters.single().isWatched)
    }

    @Test fun refreshAndActionShareSerializationSoTheLastServerResultWins() = scenario {
        seed()
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        handler = { request ->
            if (request.method == HttpMethod.Get) {
                entered.complete(Unit); release.await()
                respond("""{"clusters":[{"id":42,"name":"Name","summary":"Summary","problemStatement":"Problem","postCount":2,"category":"uncategorized","isWatched":false,"date":null}],"nextCursor":null}""", HttpStatusCode.OK, headers)
            } else respond("""{"is_watched":true}""", HttpStatusCode.OK, headers)
        }
        val repository = BrowseRepositoryImpl(sessions, BrowseRemoteDataSource(client, config), browse, sync)
        coroutineScope {
            val refresh = async { repository.refresh(OpportunityCategoryFilter.ALL, SortType.NEWEST) }
            entered.await()
            val mutation = async { actions.setWatched("user", 42, true) }
            release.complete(Unit)
            refresh.await(); mutation.await()
        }
        assertMembership(true)
    }

    private suspend fun seed(user: String = "user") {
        val card = Opportunity(42, "Name", "Summary", "Problem", 2, false, null)
        browse.replaceFirstPage(user, OpportunityCategoryFilter.ALL, SortType.NEWEST, BrowsePage(listOf(card), null))
        discovery.replaceDashboard(user, DiscoveryDashboard(card, listOf(DiscoveryCollection(OpportunityCategory.BUILD_AND_TOOLING, listOf(card)))))
    }
    private suspend fun assertMembership(expected: Boolean) {
        assertEquals(expected, browse.observeBrowse("user", OpportunityCategoryFilter.ALL, SortType.NEWEST).first().clusters.single().isWatched)
        val snapshot = discovery.observeDashboard("user").first()
        assertEquals(expected, snapshot.dashboard.featuredCluster?.isWatched)
        assertEquals(expected, snapshot.dashboard.collections.single().clusters.single().isWatched)
    }
    private fun scenario(block: suspend () -> Unit) = runBlocking { withTimeout(15_000) { block() } }
    private class Sessions : AuthSessionProvider {
        val user = MutableStateFlow<String?>("user")
        var session: AuthSession? = AuthSession("user", "test-user-token")
        override fun observeUserId() = user
        override suspend fun currentSession() = session
        fun change(next: String) { user.value = next; session = AuthSession(next, "test-other-token") }
    }
    private companion object {
        val headers = headersOf(HttpHeaders.ContentType, "application/json")
        val config = OpportunitiesApiConfig("https://opportunities.example")
    }
}
