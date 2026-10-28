package com.example.problemfinder.data.opportunities.impl.di

import android.content.Context
import com.example.problemfinder.data.opportunities.impl.db.dao.OpportunityDao
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import com.example.problemfinder.data.opportunities.impl.db.dao.BrowseDao
import com.example.problemfinder.data.opportunities.impl.db.dao.DiscoveryDao
import com.example.problemfinder.data.opportunities.impl.db.OpportunitiesDatabase
import com.example.problemfinder.data.opportunities.impl.db.dao.OpportunityDetailsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object OpportunitiesDatabaseModule {
    @Provides
    @Singleton
    fun provideOpportunitiesDatabase(
        @ApplicationContext context: Context
    ): OpportunitiesDatabase =
        Room.databaseBuilder<OpportunitiesDatabase>(
            context,
            "opportunities.db"
        )
            .setDriver(AndroidSQLiteDriver())
            .build()

    @Provides
    fun provideOpportunityDao(
        database: OpportunitiesDatabase
    ): OpportunityDao = database.opportunityDao()

    @Provides
    fun provideBrowseDao(
        database: OpportunitiesDatabase
    ): BrowseDao = database.browseDao()

    @Provides
    fun provideDiscoveryDao(
        database: OpportunitiesDatabase
    ): DiscoveryDao = database.discoveryDao()

    @Provides
    fun provideDiscoveryDetailsDao(
        database: OpportunitiesDatabase
    ): OpportunityDetailsDao = database.discoveryDetailsDao()
}
