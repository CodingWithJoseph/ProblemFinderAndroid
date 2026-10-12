package com.example.problemfinder.core.common

abstract class UseCase<in Params, out Result> {
    suspend operator fun invoke(params: Params): Result = execute(params)
    protected abstract suspend fun execute(params: Params): Result
}
