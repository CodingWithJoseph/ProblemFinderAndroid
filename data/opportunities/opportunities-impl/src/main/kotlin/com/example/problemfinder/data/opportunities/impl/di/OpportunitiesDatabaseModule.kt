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

/** Gives all opportunity data sources the same persistent database and its three DAO views. */
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

// TODO [DETAILS-2 / SEARCH-2] After the new @Dao interfaces are registered on OpportunitiesDatabase,
// provide their instances here and inject them into the new local sources. Preserve existing DAO
// bindings and the shared opportunities.db file; constructor injection alone cannot create a DAO.
