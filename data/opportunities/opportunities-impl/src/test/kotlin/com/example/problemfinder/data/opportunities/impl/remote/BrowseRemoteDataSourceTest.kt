package com.example.problemfinder.data.opportunities.impl.remote

import com.example.problemfinder.core.common.shared.OpportunityCategoryFilter
import com.example.problemfinder.core.common.shared.SortType
import com.example.problemfinder.data.opportunities.impl.model.OpportunitiesApiConfig
import com.example.problemfinder.data.opportunities.impl.model.dto.BrowsePageDto
import com.example.problemfinder.data.opportunities.impl.model.dto.OpportunityDto
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.JsonConvertException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

class BrowseRemoteDataSourceTest {
    @Test
    fun `first page sends authenticated request and decodes complete cards and optional dates`() = runTest {
        withSource({ request ->
            assertEquals(HttpMethod.Get, request.method)
            assertEquals("https", request.url.protocol.name)
            assertEquals("browse.example", request.url.host)
            assertEquals("/clusters/browse/page", request.url.encodedPath)
            assertEquals("Bearer test-access-token", request.headers[HttpHeaders.Authorization])
            assertEquals(setOf("category", "sort", "limit"), request.url.parameters.names())
            assertEquals("all", request.url.parameters["category"])
            assertEquals("newest", request.url.parameters["sort"])
            assertEquals("20", request.url.parameters["limit"])
            assertFalse(request.url.parameters.contains("cursor"))
            respond(FIRST_PAGE_JSON, HttpStatusCode.OK, JSON_HEADERS)
        }) { source ->
            val page = source.fetchBrowsePage(
                category = OpportunityCategoryFilter.ALL,
                sortType = SortType.NEWEST,
                accessToken = "test-access-token",
                cursor = null
            )

            assertEquals(
                BrowsePageDto(
                    clusters = listOf(
                        OpportunityDto(
                            id = 30,
                            name = "Maintenance requests",
                            summary = "A complete opportunity summary for the preview.",
                            problemStatement = "How can teams track requests?",
                            postCount = 3,
                            category = "uncategorized",
                            isWatched = true,
                            date = "2026-10-10T12:00:00.123456Z"
                        ),
                        OpportunityDto(
                            id = 20,
                            name = "Unclassified opportunity",
                            summary = "A complete opportunity summary for the preview.",
                            problemStatement = "Teams need a reliable way to complete this task.",
                            postCount = 2,
                            category = "uncategorized",
                            isWatched = false,
                            date = null
                        )
                    ),
                    nextCursor = "next-page-token"
                ),
                page
            )
        }
    }

    @Test
    fun `next page sends opaque cursor unchanged and respects explicit limit`() = runTest {
        val cursor = "opaque+/= cursor?"
        withSource({ request ->
            assertEquals(cursor, request.url.parameters["cursor"])
            assertEquals("7", request.url.parameters["limit"])
            respond(EMPTY_PAGE_JSON, HttpStatusCode.OK, JSON_HEADERS)
        }) { source ->
            val page = source.fetchBrowsePage(
                category = OpportunityCategoryFilter.ALL,
                sortType = SortType.NEWEST,
                accessToken = "test-access-token",
                cursor = cursor,
                limit = 7
            )

            assertEquals(emptyList<OpportunityDto>(), page.clusters)
            assertNull(page.nextCursor)
        }
    }

    @Test
    fun `empty first page decodes as end of results`() = runTest {
        withSource({
            respond(EMPTY_PAGE_JSON, HttpStatusCode.OK, JSON_HEADERS)
        }) { source ->
            assertEquals(BrowsePageDto(emptyList(), null), source.fetchFirstPage())
        }
    }

    @Test
    fun `category filters are encoded using backend tokens`() = runTest {
        val categories = listOf(
            OpportunityCategoryFilter.ALL to "all",
            OpportunityCategoryFilter.BUILD_AND_TOOLING to "build_and_tooling",
            OpportunityCategoryFilter.DATA_AND_APIS to "data_and_apis",
            OpportunityCategoryFilter.WORKFLOW_AND_AUTOMATION to "workflow_and_automation",
            OpportunityCategoryFilter.UNCATEGORIZED to "uncategorized",
        )
        var expectedToken = ""
        withSource({ request ->
            assertEquals(expectedToken, request.url.parameters["category"])
            respond(EMPTY_PAGE_JSON, HttpStatusCode.OK, JSON_HEADERS)
        }) { source ->
            for ((category, token) in categories) {
                expectedToken = token
                source.fetchBrowsePage(category, SortType.NEWEST, "test-access-token", null)
            }
        }
    }

    @Test
    fun `sort values are encoded using backend tokens`() = runTest {
        val sorts = listOf(
            SortType.NEWEST to "newest",
            SortType.OLDEST to "oldest",
            SortType.LARGEST to "largest",
            SortType.SMALLEST to "smallest"
        )
        var expectedToken = ""
        withSource({ request ->
            assertEquals(expectedToken, request.url.parameters["sort"])
            respond(EMPTY_PAGE_JSON, HttpStatusCode.OK, JSON_HEADERS)
        }) { source ->
            for ((sort, token) in sorts) {
                expectedToken = token
                source.fetchBrowsePage(OpportunityCategoryFilter.ALL, sort, "test-access-token", null)
            }
        }
    }

    @Test
    fun `unauthorized response remains an HTTP failure`() = runTest {
        assertHttpClientFailure(HttpStatusCode.Unauthorized, "Invalid or expired access token")
    }

    @Test
    fun `rejected cursor remains an HTTP failure`() = runTest {
        assertHttpClientFailure(HttpStatusCode.UnprocessableEntity, "Invalid browse cursor")
    }

    @Test
    fun `server failure remains an HTTP failure`() = runTest {
        withSource({
            respond("""{"detail":"Service unavailable"}""", HttpStatusCode.ServiceUnavailable, JSON_HEADERS)
        }) { source ->
            val error = expectFailure<ServerResponseException> { source.fetchFirstPage() }
            assertEquals(HttpStatusCode.ServiceUnavailable, error.response.status)
        }
    }

    @Test
    fun `missing required page fields fail decoding`() = runTest {
        withSource({
            respond("""{"nextCursor":null}""", HttpStatusCode.OK, JSON_HEADERS)
        }) { source ->
            val error = expectFailure<JsonConvertException> { source.fetchFirstPage() }
            assertTrue(error.cause is SerializationException)
        }
    }

    @Test
    fun `malformed JSON fails decoding`() = runTest {
        withSource({
            respond("""{"clusters": """, HttpStatusCode.OK, JSON_HEADERS)
        }) { source ->
            val error = expectFailure<JsonConvertException> { source.fetchFirstPage() }
            assertTrue(error.cause is SerializationException)
        }
    }

    @Test
    fun `invalid required field type fails decoding`() = runTest {
        withSource({
            respond(FIRST_PAGE_JSON.replace("\"postCount\": 3", "\"postCount\": \"many\""), HttpStatusCode.OK, JSON_HEADERS)
        }) { source ->
            val error = expectFailure<JsonConvertException> { source.fetchFirstPage() }
            assertTrue(error.cause is SerializationException)
        }
    }

    @Test
    fun `network failure propagates instead of becoming an empty page`() = runTest {
        val networkError = IOException("Connection interrupted")
        withSource({ throw networkError }) { source ->
            val error = expectFailure<IOException> { source.fetchFirstPage() }
            assertEquals(networkError.message, error.message)
        }
    }

    private suspend fun assertHttpClientFailure(status: HttpStatusCode, detail: String) {
        withSource({
            respond("""{"detail":"$detail"}""", status, JSON_HEADERS)
        }) { source ->
            val error = expectFailure<ClientRequestException> { source.fetchFirstPage() }
            assertEquals(status, error.response.status)
        }
    }

    private suspend fun BrowseRemoteDataSource.fetchFirstPage(): BrowsePageDto = fetchBrowsePage(
        category = OpportunityCategoryFilter.ALL,
        sortType = SortType.NEWEST,
        accessToken = "test-access-token",
        cursor = null
    )

    @Test
    fun `required card fields reject null and missing values`() = runTest {
        for (field in listOf("name", "summary", "problemStatement", "category")) {
            val fieldPattern = Regex(""""$field"\s*:\s*"[^"]*"""")
            val original = requireNotNull(fieldPattern.find(FIRST_PAGE_JSON)).value
            for (body in listOf(
                FIRST_PAGE_JSON.replace(original, "\"$field\": null"),
                FIRST_PAGE_JSON.replace("$original,", ""),
            )) {
                withSource({ respond(body, HttpStatusCode.OK, JSON_HEADERS) }) { source ->
                    expectFailure<JsonConvertException> { source.fetchFirstPage() }
                }
            }
        }
    }

    private suspend fun withSource(
        handler: MockRequestHandler,
        test: suspend (BrowseRemoteDataSource) -> Unit
    ) {
        val client = createOpportunitiesHttpClient(MockEngine) {
            addHandler(handler)
        }
        try {
            test(BrowseRemoteDataSource(client, OpportunitiesApiConfig("https://browse.example/")))
        } finally {
            client.close()
        }
    }

    private suspend inline fun <reified T : Throwable> expectFailure(
        crossinline operation: suspend () -> Any?
    ): T {
        try {
            operation()
        } catch (error: Throwable) {
            if (error is T) return error
            throw error
        }
        fail("Expected ${T::class.java.simpleName}")
        error("Expected operation to fail")
    }

    private companion object {
        val JSON_HEADERS = headersOf(HttpHeaders.ContentType, "application/json")
        const val EMPTY_PAGE_JSON = """{"clusters":[],"nextCursor":null}"""
        const val FIRST_PAGE_JSON = """
            {
              "clusters": [
                {
                  "id": 30,
                  "name": "Maintenance requests",
                  "summary": "A complete opportunity summary for the preview.",
                  "problemStatement": "How can teams track requests?",
                  "postCount": 3,
                  "category": "uncategorized",
                  "isWatched": true,
                  "date": "2026-10-10T12:00:00.123456Z",
                  "futureClusterField": "ignored"
                },
                {
                  "id": 20,
                  "name": "Unclassified opportunity",
                  "summary": "A complete opportunity summary for the preview.",
                  "problemStatement": "Teams need a reliable way to complete this task.",
                  "postCount": 2,
                  "category": "uncategorized",
                  "isWatched": false,
                  "date": null
                }
              ],
              "nextCursor": "next-page-token",
              "futurePageField": "ignored"
            }
        """
    }
}
