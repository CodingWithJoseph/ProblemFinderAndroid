package com.example.problemfinder.core.mvi

/**
 * Lifecycle of one request, with independent success and error types. Features can keep content
 * outside this value so a refresh failure does not have to erase already displayed data.
 */
sealed interface RequestState<out T, out E> {
    object Idle: RequestState<Nothing, Nothing>
    object Loading: RequestState<Nothing, Nothing>
    data class Success<T>(val value: T): RequestState<T, Nothing>
    data class Error<E>(val error: E): RequestState<Nothing, E>
}
