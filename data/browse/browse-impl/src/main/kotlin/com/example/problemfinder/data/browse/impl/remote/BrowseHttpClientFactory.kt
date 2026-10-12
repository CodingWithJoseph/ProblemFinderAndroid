package com.example.problemfinder.data.browse.impl.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

internal fun <T : HttpClientEngineConfig> createBrowseHttpClient(
    engineFactory: HttpClientEngineFactory<T>,
    configureEngine: T.() -> Unit = {}
): HttpClient = HttpClient(engineFactory) {
    engine(configureEngine)
    expectSuccess = true

    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
        })
    }

    install(HttpTimeout) {
        requestTimeoutMillis = 30_000
        connectTimeoutMillis = 30_000
        socketTimeoutMillis = 30_000
    }
}
