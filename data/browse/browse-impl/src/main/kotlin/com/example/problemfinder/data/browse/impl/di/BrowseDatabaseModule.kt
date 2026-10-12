package com.example.problemfinder.data.browse.impl.di

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import com.example.problemfinder.data.browse.impl.local.BrowseDao
import com.example.problemfinder.data.browse.impl.local.BrowseDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BrowseDatabaseModule {
    @Provides
    @Singleton
    fun provideBrowseDatabase(
        @ApplicationContext context: Context
    ): BrowseDatabase =
        Room.databaseBuilder<BrowseDatabase>(
            context,
            "browse.db"
        )
            .setDriver(AndroidSQLiteDriver())
            .build()

    @Provides
    fun provideBrowseDao(
        database: BrowseDatabase
    ): BrowseDao = database.browseDao()
}
