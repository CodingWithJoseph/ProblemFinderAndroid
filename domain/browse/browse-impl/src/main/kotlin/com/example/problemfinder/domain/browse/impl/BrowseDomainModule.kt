package com.example.problemfinder.domain.browse.impl

import com.example.problemfinder.domain.browse.api.LoadNextBrowsePageUseCase
import com.example.problemfinder.domain.browse.api.ObserveBrowseUseCase
import com.example.problemfinder.domain.browse.api.RefreshBrowseUseCase
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

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
