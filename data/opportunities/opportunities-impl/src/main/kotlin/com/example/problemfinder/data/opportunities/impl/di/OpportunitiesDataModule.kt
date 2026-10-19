package com.example.problemfinder.data.opportunities.impl.di

import com.example.problemfinder.data.opportunities.api.BrowseRepository
import com.example.problemfinder.data.opportunities.api.DiscoveryRepository
import com.example.problemfinder.data.opportunities.impl.BrowseRepositoryImpl
import com.example.problemfinder.data.opportunities.impl.DiscoveryRepositoryImpl
import com.example.problemfinder.data.opportunities.api.OpportunityActionsRepository
import com.example.problemfinder.data.opportunities.impl.OpportunityActionsRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class OpportunitiesDataModule {
    @Binds
    @Singleton
    abstract fun bindOpportunityActionsRepository(implementation: OpportunityActionsRepositoryImpl): OpportunityActionsRepository


    @Binds
    @Singleton
    abstract fun bindBrowseRepository(implementation: BrowseRepositoryImpl): BrowseRepository

    @Binds
    @Singleton
    abstract fun bindDiscoveryRepository(implementation: DiscoveryRepositoryImpl): DiscoveryRepository
}
