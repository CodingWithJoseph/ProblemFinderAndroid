package com.example.problemfinder.data.opportunities.impl.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** Shares HTTP policy between the Android engine in production and mock engines in tests. */
internal fun <T : HttpClientEngineConfig> createOpportunitiesHttpClient(
    engineFactory: HttpClientEngineFactory<T>,
    configureEngine: T.() -> Unit = {}
): HttpClient = HttpClient(engineFactory) {
    engine(configureEngine)
    // Non-success status codes must remain failures rather than decoding as empty domain content.
    expectSuccess = true

    install(ContentNegotiation) {
        json(Json {
            // Permit additive backend fields while still rejecting invalid required DTO fields.
            ignoreUnknownKeys = true
        })
    }

    install(HttpTimeout) {
        requestTimeoutMillis = 30_000
        connectTimeoutMillis = 30_000
        socketTimeoutMillis = 30_000
    }
}
