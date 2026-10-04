package com.example.problemfinder.domain.authentication.impl

import com.example.problemfinder.domain.authentication.api.AuthUseCases
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent


@Module
@InstallIn(SingletonComponent::class)
abstract class AuthDomainModule {
    @Binds
    abstract fun bindAuthUseCases(implementation: DefaultAuthUseCases): AuthUseCases
}
