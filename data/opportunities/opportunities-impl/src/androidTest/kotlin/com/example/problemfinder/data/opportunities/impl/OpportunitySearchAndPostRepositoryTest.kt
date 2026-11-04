package com.example.problemfinder.data.opportunities.impl

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.problemfinder.core.model.search.OpportunitySearchQuery
import com.example.problemfinder.data.authentication.api.AuthSession
import com.example.problemfinder.data.authentication.api.AuthSessionProvider
import com.example.problemfinder.data.opportunities.impl.db.OpportunitiesDatabase
import com.example.problemfinder.data.opportunities.impl.local.OpportunityDetailsLocalDataSource
import com.example.problemfinder.data.opportunities.impl.local.OpportunitySearchLocalDataSource
import com.example.problemfinder.data.opportunities.impl.model.OpportunitiesApiConfig
import com.example.problemfinder.data.opportunities.impl.remote.*
import com.example.problemfinder.data.opportunities.impl.util.OpportunitySyncCoordinator
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OpportunitySearchAndPostRepositoryTest {
    @Test
    fun assistantPreparationDoesNotExecuteAndSearchContinuesFromTheServerOffset() = runBlocking {
        val offsets = mutableListOf<Int>()
        val client = createOpportunitiesHttpClient(MockEngine) {
            addHandler { request ->
                assertEquals("Bearer fixture-token", request.headers[HttpHeaders.Authorization])
                val response = when (request.url.encodedPath) {
                    "/clusters/search/options" -> """{"sources":["github"],"communities":["tools"]}"""
                    "/clusters/search/interpret" -> {
                        assertEquals(HttpMethod.Post, request.method)
                        """{"draft":{"query":"build failure","limit":2},"confirmation":"Review these filters",
                            "assumptions":[],"unsupported":[],"needs_clarification":false,
                            "needs_confirmation":true,"fallback_used":true}"""
                    }
                    "/clusters/search/query" -> {
                        assertEquals(HttpMethod.Post, request.method)
                        val body = Json.parseToJsonElement((request.body as OutgoingContent.ByteArrayContent).bytes().decodeToString()).jsonObject
                        assertEquals("build failure", body.getValue("query").jsonPrimitive.content)
                        val offset = body["offset"]?.jsonPrimitive?.int ?: 0
                        offsets.add(offset)
                        when (offset) {
                            0 -> resultPage(0, listOf(8, 3), 2)
                            2 -> resultPage(2, listOf(4), null)
                            else -> error("Unexpected offset")
                        }
                    }
                    else -> error("Unexpected search endpoint")
                }
                respond(response, headers = headersOf(HttpHeaders.ContentType, "application/json"))
            }
        }
        val db = database()
        try {
            val repository = OpportunitySearchRepositoryImpl(sessions, OpportunitySearchRemoteDataSource(client, config),
                OpportunitySearchLocalDataSource(db), OpportunitySyncCoordinator())
            assertEquals(listOf("github"), repository.filterOptions().sources)
            val draft = repository.prepareDraft("Find build failures", null)
            assertTrue(draft.fallbackUsed)
            assertTrue(offsets.isEmpty())
            assertFalse(repository.observeSearch(draft.query).first { it.cacheReady }.hasSynced)
            repository.refreshSearch(draft.query)
            repository.loadNextSearchPage(draft.query)
            repository.loadNextSearchPage(draft.query)
            val result = repository.observeSearch(draft.query).first { it.cacheReady }
            assertEquals(listOf(0, 2), offsets)
            assertEquals(listOf(8, 3, 4), result.items.map { it.id })
            assertNull(result.nextOffset)
        } finally {
            client.close()
            db.close()
        }
    }

    @Test
    fun coldPostLookupKeepsUuidAndFindsAnItemAfterPageOne() = runBlocking {
        val postId = "f578e554-8fb7-4243-bc95-1541ca495746"
        val allIds = (1..200).map { "fixture-" + it } + postId
        val pages = mutableListOf<Int>()
        val client = createOpportunitiesHttpClient(MockEngine) {
            addHandler { request ->
                assertEquals("/clusters/42", request.url.encodedPath)
                assertEquals("200", request.url.parameters["pageSize"])
                val page = request.url.parameters["page"]!!.toInt()
                pages.add(page)
                val posts = if (page == 1) "[]" else """[{"id":"$postId","title":"Selected post","body":"Full post"}]"""
                val response = """{"id":42,"authorCount":1,"communityCount":1,"sourceBreakdown":[],"postVolumeByDate":[],
                    "topEvidence":[],"posts":$posts,"page":$page,"pageSize":200,"totalPosts":201,
                    "postIds":${Json.encodeToString(allIds)}}"""
                respond(response, headers = headersOf(HttpHeaders.ContentType, "application/json"))
            }
        }
        val db = database()
        try {
            val repository = OpportunityDetailsRepositoryImpl(sessions, OpportunityDetailsRemoteDataSource(client, config),
                OpportunityDetailsLocalDataSource(db, db.discoveryDetailsDao()), OpportunitySyncCoordinator())
            assertEquals("Full post", repository.loadPost(42, postId, "account")?.body)
            assertEquals(listOf(1, 2), pages)
        } finally {
            client.close()
            db.close()
        }
    }

    private val config = OpportunitiesApiConfig("https://opportunities.example")
    private val sessions = object : AuthSessionProvider {
        override fun observeUserId() = flowOf("account")
        override suspend fun currentSession() = AuthSession("account", "fixture-token")
    }

    private fun database(): OpportunitiesDatabase =
        Room.inMemoryDatabaseBuilder<OpportunitiesDatabase>(ApplicationProvider.getApplicationContext<Context>())
            .setDriver(AndroidSQLiteDriver()).build()

    private fun resultPage(offset: Int, ids: List<Int>, next: Int?): String {
        val data = ids.joinToString(",", "[", "]") {
            """{"id":$it,"name":"Name","summary":"Summary","problemStatement":"Problem","post_count":1,"is_watched":false}"""
        }
        return """{"data":$data,"total":3,"applied_filters":{"query":"build failure","min_posts":1,"sort":"relevance"},
            "pagination":{"limit":2,"offset":$offset,"returned":${ids.size},"has_more":${next != null},"next_offset":$next}}"""
    }
}
