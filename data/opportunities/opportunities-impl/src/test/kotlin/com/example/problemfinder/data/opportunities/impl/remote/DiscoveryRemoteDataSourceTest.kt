package com.example.problemfinder.data.opportunities.impl.remote

import com.example.problemfinder.data.opportunities.impl.model.OpportunitiesApiConfig
import com.example.problemfinder.data.opportunities.impl.model.dto.DiscoveryDashboardDto
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

class DiscoveryRemoteDataSourceTest {
    @Test
    fun `dashboard sends authenticated GET to the backend route and decodes nested cards`() = runTest {
        withSource({ request ->
            assertEquals(HttpMethod.Get, request.method)
            assertEquals("https", request.url.protocol.name)
            assertEquals("browse.example", request.url.host)
            assertEquals("/clusters/discovery", request.url.encodedPath)
            assertEquals("Bearer test-access-token", request.headers[HttpHeaders.Authorization])
            assertTrue(request.url.parameters.isEmpty())
            respond(DASHBOARD_JSON, HttpStatusCode.OK, JSON_HEADERS)
        }) { source ->
            val dashboard = source.fetchDashboard("test-access-token")
            val featured = requireNotNull(dashboard.featured)
            assertEquals(30, featured.id)
            assertEquals("build_and_tooling", featured.category)
            assertEquals("Build tools fail", featured.name)
            assertEquals("A problem statement", featured.problemStatement)
            assertEquals("A summary", featured.summary)
            assertEquals(14, featured.postCount)
            assertTrue(featured.isWatched)
            assertEquals("2026-10-17T12:00:00Z", featured.date)
            assertEquals("build_and_tooling", dashboard.collections.single().category)
            assertEquals(listOf(featured), dashboard.collections.single().clusters)
        }
    }

    @Test
    fun `empty dashboard is a successful decoded response`() = runTest {
        withSource({
            respond("""{"featured":null,"collections":[]}""", HttpStatusCode.OK, JSON_HEADERS)
        }) { source ->
            assertEquals(
                DiscoveryDashboardDto(featured = null, collections = emptyList()),
                source.fetchDashboard("test-access-token"),
            )
        }
    }

    @Test
    fun `missing activity date remains null`() = runTest {
        withSource({
            respond(
                DASHBOARD_JSON.replace("\"date\": \"2026-10-17T12:00:00Z\"", "\"date\": null"),
                HttpStatusCode.OK,
                JSON_HEADERS,
            )
        }) { source ->
            val dashboard = source.fetchDashboard("test-access-token")
            assertNull(requireNotNull(dashboard.featured).date)
            assertNull(dashboard.collections.single().clusters.single().date)
        }
    }

    @Test
    fun `unauthorized response remains an HTTP failure`() = runTest {
        withSource({
            respond("""{"detail":"Missing credentials"}""", HttpStatusCode.Unauthorized, JSON_HEADERS)
        }) { source ->
            val error = expectFailure<ClientRequestException> {
                source.fetchDashboard("test-access-token")
            }
            assertEquals(HttpStatusCode.Unauthorized, error.response.status)
        }
    }

    @Test
    fun `server failure does not become an empty dashboard`() = runTest {
        withSource({
            respond("""{"detail":"Unavailable"}""", HttpStatusCode.ServiceUnavailable, JSON_HEADERS)
        }) { source ->
            val error = expectFailure<ServerResponseException> {
                source.fetchDashboard("test-access-token")
            }
            assertEquals(HttpStatusCode.ServiceUnavailable, error.response.status)
        }
    }

    @Test
    fun `null required summary fails decoding`() = runTest {
        withSource({
            respond(
                DASHBOARD_JSON.replace("\"summary\": \"A summary\"", "\"summary\": null"),
                HttpStatusCode.OK,
                JSON_HEADERS,
            )
        }) { source ->
            expectFailure<JsonConvertException> { source.fetchDashboard("test-access-token") }
        }
    }

    @Test
    fun `incomplete or malformed responses fail decoding`() = runTest {
        val invalidResponses = listOf(
            """{"featured":null}""",
            DASHBOARD_JSON.replace("\"summary\": \"A summary\",", ""),
            DASHBOARD_JSON.replace("\"postCount\": 14", "\"postCount\": \"many\""),
            """{"featured":""",
        )
        for (body in invalidResponses) {
            withSource({
                respond(body, HttpStatusCode.OK, JSON_HEADERS)
            }) { source ->
                val error = expectFailure<JsonConvertException> {
                    source.fetchDashboard("test-access-token")
                }
                assertTrue(error.cause is SerializationException)
            }
        }
    }

    @Test
    fun `network failure propagates`() = runTest {
        val failure = IOException("Connection interrupted")
        withSource({ throw failure }) { source ->
            val error = expectFailure<IOException> {
                source.fetchDashboard("test-access-token")
            }
            assertEquals(failure.message, error.message)
        }
    }

    @Test
    fun `required card fields reject null and missing values`() = runTest {
        for (field in listOf("name", "summary", "problemStatement", "category")) {
            val fieldPattern = Regex(""""$field"\s*:\s*"[^"]*"""")
            val original = requireNotNull(fieldPattern.find(DASHBOARD_JSON)).value
            for (body in listOf(
                DASHBOARD_JSON.replace(original, "\"$field\": null"),
                DASHBOARD_JSON.replace("$original,", ""),
            )) {
                withSource({ respond(body, HttpStatusCode.OK, JSON_HEADERS) }) { source ->
                    expectFailure<JsonConvertException> { source.fetchDashboard("test-access-token") }
                }
            }
        }
    }

    private suspend fun withSource(
        handler: MockRequestHandler,
        test: suspend (DiscoveryRemoteDataSource) -> Unit,
    ) {
        val client = createOpportunitiesHttpClient(MockEngine) {
            addHandler(handler)
        }
        try {
            test(DiscoveryRemoteDataSource(client, OpportunitiesApiConfig("https://browse.example/")))
        } finally {
            client.close()
        }
    }

    private suspend inline fun <reified T : Throwable> expectFailure(
        crossinline operation: suspend () -> Any?,
    ): T {
        try {
            operation()
        } catch (error: Throwable) {
            if (error is T) return error
            throw error
        }
        fail("Expected operation to fail")
        error("Expected operation to fail")
    }

    private companion object {
        val JSON_HEADERS = headersOf(HttpHeaders.ContentType, "application/json")
        const val CARD_JSON = """
            {
                "id": 30,
                "category": "build_and_tooling",
                "name": "Build tools fail",
                "problemStatement": "A problem statement",
                "summary": "A summary",
                "postCount": 14,
                "isWatched": true,
                "date": "2026-10-17T12:00:00Z",
                "futureCardField": "ignored"
            }
        """
        val DASHBOARD_JSON = """
            {
                "featured": $CARD_JSON,
                "collections": [
                    {
                        "category": "build_and_tooling",
                        "clusters": [$CARD_JSON]
                    }
                ],
                "futureDashboardField": "ignored"
            }
        """
    }
}
