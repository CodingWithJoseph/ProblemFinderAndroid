package com.example.problemfinder.feature.authentication.impl.forgotpassword

import com.example.problemfinder.feature.authentication.impl.AuthUIError


data class ForgotPasswordState(
    val email: String = "",
    val isLoading: Boolean = false,
    val error: AuthUIError? = null
)

sealed interface ForgotPasswordIntent {
    data class EmailChanged(val value: String): ForgotPasswordIntent
    data object Submit: ForgotPasswordIntent
    data object SignInClicked: ForgotPasswordIntent
}

sealed interface ForgotPasswordEffect {
    data object OpenSignIn: ForgotPasswordEffect
    data object EmailSent: ForgotPasswordEffect
}
