package com.example.problemfinder.data.authentication.impl

import com.example.problemfinder.data.authentication.api.AuthRepository
import com.example.problemfinder.data.authentication.api.AuthSessionProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Supplies shared Supabase adapters to callers that depend only on the authentication APIs. */
@Module
@InstallIn(SingletonComponent::class)
abstract class AuthDataModule {
    @Binds
    @Singleton
    abstract fun bindAuthRepository(repository: SupabaseAuthRepository): AuthRepository

    @Binds
    @Singleton
    abstract fun bindAuthSessionProvider(provider: SupabaseAuthSessionProvider): AuthSessionProvider
}
