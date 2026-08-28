package com.example.problemfinder.data.problems.impl.di

import com.example.problemfinder.data.problems.api.ProblemRepository
import com.example.problemfinder.data.problems.impl.DefaultProblemRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ProblemsDataModule {
    @Binds
    @Singleton
    abstract fun bindProblemRepository(
        repository: DefaultProblemRepository,
    ): ProblemRepository
}

