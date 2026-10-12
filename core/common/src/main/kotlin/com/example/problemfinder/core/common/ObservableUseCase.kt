package com.example.problemfinder.core.common

import kotlinx.coroutines.flow.Flow


abstract class ObservableUseCase<in Params, out Result> {

    suspend operator fun invoke(params: Params): Flow<Result> = execute(params)
    protected abstract suspend fun execute(params: Params): Flow<Result>
}
