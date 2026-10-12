package com.example.problemfinder.data.browse.impl.di

import com.example.problemfinder.data.browse.impl.model.BrowseApiConfig
import com.example.problemfinder.data.browse.impl.remote.createBrowseHttpClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BrowseNetworkModule {

    @Provides
    @Singleton
    fun provideBrowseHttpClient(): HttpClient = createBrowseHttpClient(Android)

    @Provides
    fun provideBrowseApiConfig(): BrowseApiConfig = BrowseApiConfig("https://api.useimmensity.com")
}
