package com.example.problemfinder.domain.opportunities.impl.di

import com.example.problemfinder.domain.opportunities.api.browse.LoadNextBrowsePageUseCase
import com.example.problemfinder.domain.opportunities.api.browse.ObserveBrowseUseCase
import com.example.problemfinder.domain.opportunities.api.browse.RefreshBrowseUseCase
import com.example.problemfinder.domain.opportunities.impl.browse.LoadNextBrowsePageUseCaseImpl
import com.example.problemfinder.domain.opportunities.impl.browse.ObserveBrowseUseCaseImpl
import com.example.problemfinder.domain.opportunities.impl.browse.RefreshBrowseUseCaseImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Wires feature-facing use-case contracts to their repository delegates. These use cases currently
 * add no business rules; the layer keeps feature code independent of the data implementation.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class BrowseDomainModule {
    @Binds
    abstract fun bindObserveBrowseUseCase(
        impl: ObserveBrowseUseCaseImpl,
    ): ObserveBrowseUseCase

    @Binds
    abstract fun bindRefreshBrowseUseCase(
        impl: RefreshBrowseUseCaseImpl,
    ): RefreshBrowseUseCase

    @Binds
    abstract fun bindLoadNextBrowsePageUseCase(
        impl: LoadNextBrowsePageUseCaseImpl,
    ): LoadNextBrowsePageUseCase
}
