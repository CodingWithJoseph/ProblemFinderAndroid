package com.example.problemfinder.core.common

/**
 * Callable boundary for a suspending operation. Invocation delegates directly to [execute]; it
 * adds no dispatcher switch or error handling, so implementations/callers retain those policies.
 */
abstract class UseCase<in Params, out Result> {
    suspend operator fun invoke(params: Params): Result = execute(params)
    protected abstract suspend fun execute(params: Params): Result
}
