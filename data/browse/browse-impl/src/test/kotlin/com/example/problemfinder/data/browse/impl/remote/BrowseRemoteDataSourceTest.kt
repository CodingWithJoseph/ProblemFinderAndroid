package com.example.problemfinder.data.browse.impl.remote

import com.example.problemfinder.core.common.shared.Category
import com.example.problemfinder.core.common.shared.SortType
import com.example.problemfinder.data.browse.impl.model.BrowseApiConfig
import com.example.problemfinder.data.browse.impl.model.dto.BrowsePageDto
import com.example.problemfinder.data.browse.impl.model.dto.DiscoveryClusterDto
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
    fun `first page sends authenticated request and decodes nullable fields`() = runTest {
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
                category = Category.ALL,
                sortType = SortType.NEWEST,
                accessToken = "test-access-token",
                cursor = null
            )

            assertEquals(
                BrowsePageDto(
                    clusters = listOf(
                        DiscoveryClusterDto(
                            id = 30,
                            name = "Maintenance requests",
                            summary = null,
                            problemStatement = "How can teams track requests?",
                            postCount = 3,
                            isWatched = true,
                            date = "2026-10-10T12:00:00.123456Z"
                        ),
                        DiscoveryClusterDto(
                            id = 20,
                            name = null,
                            summary = null,
                            problemStatement = null,
                            postCount = 2,
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
                category = Category.ALL,
                sortType = SortType.NEWEST,
                accessToken = "test-access-token",
                cursor = cursor,
                limit = 7
            )

            assertEquals(emptyList<DiscoveryClusterDto>(), page.clusters)
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
                source.fetchBrowsePage(Category.ALL, sort, "test-access-token", null)
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
        category = Category.ALL,
        sortType = SortType.NEWEST,
        accessToken = "test-access-token",
        cursor = null
    )

    private suspend fun withSource(
        handler: MockRequestHandler,
        test: suspend (BrowseRemoteDataSource) -> Unit
    ) {
        val client = createBrowseHttpClient(MockEngine) {
            addHandler(handler)
        }
        try {
            test(BrowseRemoteDataSource(client, BrowseApiConfig("https://browse.example/")))
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
                  "summary": null,
                  "problemStatement": "How can teams track requests?",
                  "postCount": 3,
                  "isWatched": true,
                  "date": "2026-10-10T12:00:00.123456Z",
                  "futureClusterField": "ignored"
                },
                {
                  "id": 20,
                  "name": null,
                  "summary": null,
                  "problemStatement": null,
                  "postCount": 2,
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
