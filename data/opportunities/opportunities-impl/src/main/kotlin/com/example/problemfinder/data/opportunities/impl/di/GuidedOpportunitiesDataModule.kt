package com.example.problemfinder.data.opportunities.impl.di

import com.example.problemfinder.data.opportunities.api.OpportunityDetailsRepository
import com.example.problemfinder.data.opportunities.api.OpportunitySearchRepository
import com.example.problemfinder.data.opportunities.impl.OpportunityDetailsRepositoryImpl
import com.example.problemfinder.data.opportunities.impl.OpportunitySearchRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Wiring supplied for DETAILS-4 / SEARCH-4. Features receive use cases; use cases receive these
 * contracts. Completing a repository body replaces its unfinished behavior without changing UI DI.
 * DAO bindings are deliberately still your task in OpportunitiesDatabaseModule after Room is ready.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class GuidedOpportunitiesDataModule {
    @Binds @Singleton
    abstract fun bindDetails(implementation: OpportunityDetailsRepositoryImpl): OpportunityDetailsRepository

    @Binds @Singleton
    abstract fun bindSearch(implementation: OpportunitySearchRepositoryImpl): OpportunitySearchRepository
}
