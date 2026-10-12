package com.example.problemfinder.core.mvi

sealed interface RequestState<out T, out E> {
    object Idle: RequestState<Nothing, Nothing>
    object Loading: RequestState<Nothing, Nothing>
    data class Success<T>(val value: T): RequestState<T, Nothing>
    data class Error<E>(val error: E): RequestState<Nothing, E>
}
