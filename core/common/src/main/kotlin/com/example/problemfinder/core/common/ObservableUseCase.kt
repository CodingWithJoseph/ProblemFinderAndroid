package com.example.problemfinder.core.common

import kotlinx.coroutines.flow.Flow


/**
 * Callable boundary returning a stream. Invocation obtains the Flow; the caller must collect it
 * and owns collection lifetime. This base class does not launch work or change dispatchers.
 */
abstract class ObservableUseCase<in Params, out Result> {

    suspend operator fun invoke(params: Params): Flow<Result> = execute(params)
    protected abstract suspend fun execute(params: Params): Flow<Result>
}
