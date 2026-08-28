package com.example.problemfinder.domain.problems.impl.di

import com.example.problemfinder.domain.problems.api.SearchProblems
import com.example.problemfinder.domain.problems.impl.DefaultSearchProblems
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ProblemsDomainModule {
    @Binds
    @Singleton
    abstract fun bindSearchProblems(
        useCase: DefaultSearchProblems,
    ): SearchProblems
}

