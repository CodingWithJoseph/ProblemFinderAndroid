package com.example.problemfinder.feature.authentication.impl.signup

import com.example.problemfinder.feature.authentication.impl.AuthUIError


enum class SignUpStep { CREDENTIALS, PROFILE }

data class SignUpState(
    val step: SignUpStep = SignUpStep.CREDENTIALS,
    val firstname: String = "",
    val lastname: String = "",
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: AuthUIError? = null
)

sealed interface SignUpIntent {
    data class FirstnameChanged(val value: String): SignUpIntent
    data class LastnameChanged(val value: String): SignUpIntent
    data class EmailChanged(val value: String): SignUpIntent
    data class PasswordChanged(val value: String): SignUpIntent
    data object ContinueClicked: SignUpIntent
    data object PreviousClicked: SignUpIntent
    data object Submit: SignUpIntent
    data object SignInClicked: SignUpIntent
}

sealed interface SignUpEffect {
    data object OpenSignIn: SignUpEffect
    data object EmailSent: SignUpEffect
}
