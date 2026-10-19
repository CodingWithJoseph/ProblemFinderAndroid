package com.example.problemfinder.domain.opportunities.impl.di

import com.example.problemfinder.domain.opportunities.api.browse.LoadNextBrowsePageUseCase
import com.example.problemfinder.domain.opportunities.api.browse.ObserveBrowseUseCase
import com.example.problemfinder.domain.opportunities.api.browse.RefreshBrowseUseCase
import com.example.problemfinder.domain.opportunities.api.discovery.ObserveDiscoveryUseCase
import com.example.problemfinder.domain.opportunities.api.discovery.RefreshDiscoveryUseCase
import com.example.problemfinder.domain.opportunities.impl.browse.LoadNextBrowsePageUseCaseImpl
import com.example.problemfinder.domain.opportunities.impl.browse.ObserveBrowseUseCaseImpl
import com.example.problemfinder.domain.opportunities.impl.browse.RefreshBrowseUseCaseImpl
import com.example.problemfinder.domain.opportunities.impl.discovery.ObserveDiscoveryUseCaseImpl
import com.example.problemfinder.domain.opportunities.impl.discovery.RefreshDiscoveryUseCaseImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class DiscoveryDomainModule {
    @Binds
    abstract fun bindObserveDiscoveryUseCase(
        impl: ObserveDiscoveryUseCaseImpl,
    ): ObserveDiscoveryUseCase

    @Binds
    abstract fun bindRefreshDiscoveryUseCase(
        impl: RefreshDiscoveryUseCaseImpl,
    ): RefreshDiscoveryUseCase
}
