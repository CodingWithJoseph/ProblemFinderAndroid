package com.example.problemfinder.data.opportunities.impl.di

import com.example.problemfinder.data.opportunities.impl.model.OpportunitiesApiConfig
import com.example.problemfinder.data.opportunities.impl.remote.createOpportunitiesHttpClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object OpportunitiesNetworkModule {

    @Provides
    @Singleton
    fun provideOpportunitiesHttpClient(): HttpClient = createOpportunitiesHttpClient(Android)

    @Provides
    fun provideOpportunitiesApiConfig(): OpportunitiesApiConfig = OpportunitiesApiConfig("https://api.useimmensity.com")
}
