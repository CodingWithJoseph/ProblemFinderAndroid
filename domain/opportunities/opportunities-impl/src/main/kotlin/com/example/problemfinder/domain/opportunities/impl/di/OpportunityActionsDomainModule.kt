package com.example.problemfinder.domain.opportunities.impl.di

import com.example.problemfinder.domain.opportunities.api.actions.SetOpportunityWatchedUseCase
import com.example.problemfinder.domain.opportunities.impl.actions.SetOpportunityWatchedUseCaseImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Connects the shared watch-action contract to its repository-backed implementation. */
@Module
@InstallIn(SingletonComponent::class)
abstract class OpportunityActionsDomainModule {
    @Binds
    abstract fun bindSetOpportunityWatchedUseCase(implementation: SetOpportunityWatchedUseCaseImpl): SetOpportunityWatchedUseCase
}
