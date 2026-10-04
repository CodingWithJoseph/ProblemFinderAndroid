package com.example.problemfinder.feature.authentication.impl.signin

import com.example.problemfinder.feature.authentication.impl.AuthUIError


data class SignInState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: AuthUIError? = null
)

sealed interface SignInIntent {
    data class EmailChanged(val value: String): SignInIntent
    data class PasswordChanged(val value: String): SignInIntent
    data object Submit: SignInIntent
    data object SignUpClicked: SignInIntent
    data object ForgotPasswordClicked: SignInIntent
}

sealed interface SignInEffect {
    data object SignedIn: SignInEffect
    data object OpenSignUp: SignInEffect
    data object OpenForgotPassword: SignInEffect
}
