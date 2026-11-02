package com.example.problemfinder.data.workspaces.impl

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import com.example.problemfinder.data.workspaces.api.WorkspaceRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import javax.inject.Singleton

internal fun <T : HttpClientEngineConfig> workspaceHttpClient(factory: HttpClientEngineFactory<T>, configure: T.() -> Unit = {}): HttpClient =
    HttpClient(factory) {
        engine(configure)
        expectSuccess = true
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
            connectTimeoutMillis = 30_000
            socketTimeoutMillis = 60_000
        }
    }

@Module @InstallIn(SingletonComponent::class)
internal abstract class WorkspaceBindings {
    @Binds @Singleton abstract fun repository(implementation: WorkspaceRepositoryImpl): WorkspaceRepository
}

@Module @InstallIn(SingletonComponent::class)
internal object WorkspaceDataModule {
    @Provides @Singleton fun json(): Json = Json { ignoreUnknownKeys = true }
    @Provides @Singleton @WorkspaceNetwork fun client(): HttpClient = workspaceHttpClient(Android)
    @Provides fun config(): WorkspaceApiConfig = WorkspaceApiConfig("https://api.useimmensity.com")
    @Provides @Singleton fun database(@ApplicationContext context: Context): WorkspaceDatabase =
        Room.databaseBuilder<WorkspaceDatabase>(context, "workspaces.db").setDriver(AndroidSQLiteDriver()).build()
    @Provides fun dao(database: WorkspaceDatabase): WorkspaceCacheDao = database.documents()
}
