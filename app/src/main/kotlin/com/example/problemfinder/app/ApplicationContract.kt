package com.example.problemfinder.app

data class ApplicationState(
    val isSigningOut: Boolean = false
)

sealed interface ApplicationIntent {
    data object SignOutClicked: ApplicationIntent
}

sealed interface ApplicationEffect {
    data object SignOutFailed: ApplicationEffect
}
