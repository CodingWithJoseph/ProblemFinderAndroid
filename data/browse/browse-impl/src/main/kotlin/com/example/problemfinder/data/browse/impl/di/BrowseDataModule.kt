package com.example.problemfinder.data.browse.impl.di

import com.example.problemfinder.data.browse.api.BrowseRepository
import com.example.problemfinder.data.browse.impl.BrowseRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BrowseDataModule {

    @Binds
    @Singleton
    abstract fun bindBrowseRepository(implementation: BrowseRepositoryImpl): BrowseRepository
}
