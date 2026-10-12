package com.example.problemfinder.app

import com.example.problemfinder.core.mvi.RequestState

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
