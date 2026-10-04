package com.example.problemfinder.data.authentication.impl

import com.example.problemfinder.data.authentication.api.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthDataModule {
    @Binds
    @Singleton
    abstract fun bindAuthRepository(repository: SupabaseAuthRepository): AuthRepository
}
