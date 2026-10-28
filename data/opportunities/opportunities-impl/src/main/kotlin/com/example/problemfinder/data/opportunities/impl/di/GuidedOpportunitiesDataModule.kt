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

@Module
@InstallIn(SingletonComponent::class)
abstract class GuidedOpportunitiesDataModule {
    @Binds @Singleton
    abstract fun bindDetails(implementation: OpportunityDetailsRepositoryImpl): OpportunityDetailsRepository

    @Binds @Singleton
    abstract fun bindSearch(implementation: OpportunitySearchRepositoryImpl): OpportunitySearchRepository
}
