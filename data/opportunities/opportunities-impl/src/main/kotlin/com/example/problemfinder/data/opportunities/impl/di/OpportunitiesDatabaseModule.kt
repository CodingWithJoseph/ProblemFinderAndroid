package com.example.problemfinder.data.opportunities.impl.di

import android.content.Context
import com.example.problemfinder.data.opportunities.impl.db.OpportunityDao
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import com.example.problemfinder.data.opportunities.impl.db.BrowseDao
import com.example.problemfinder.data.opportunities.impl.db.DiscoveryDao
import com.example.problemfinder.data.opportunities.impl.db.OpportunitiesDatabase
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
    fun provideOpportunityDao(database: OpportunitiesDatabase): OpportunityDao = database.opportunityDao()

    @Provides
    fun provideBrowseDao(
        database: OpportunitiesDatabase
    ): BrowseDao = database.browseDao()

    @Provides
    fun provideDiscoveryDao(
        database: OpportunitiesDatabase
    ): DiscoveryDao = database.discoveryDao()
}
