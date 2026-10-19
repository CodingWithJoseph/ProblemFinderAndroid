package com.example.problemfinder.app

import com.example.problemfinder.core.mvi.RequestState

/**
 * Shell state shared by navigation and the sign-out menu. Success(null) is a completed read without
 * account identity; Loading and Error mean the read has not established whether an account exists.
 */
data class ApplicationState(
    val isSigningOut: Boolean = false,
    val accountRequest: RequestState<String?, Unit> = RequestState.Loading,
)

sealed interface ApplicationIntent {
    data object SignOutClicked : ApplicationIntent
    data object RetryAccountRead : ApplicationIntent
}

sealed interface ApplicationEffect {
    data object SignOutFailed : ApplicationEffect
}
