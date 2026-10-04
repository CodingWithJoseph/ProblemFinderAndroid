package com.example.problemfinder.feature.authentication.impl.resetpassword

import com.example.problemfinder.feature.authentication.impl.AuthUIError


data class ResetPasswordState(
    val password: String = "",
    val confirmationPassword: String = "",
    val isLoading: Boolean = false,
    val error: AuthUIError? = null
)

sealed interface ResetPasswordIntent {
    data class PasswordChanged(val value: String): ResetPasswordIntent
    data class ConfirmationPasswordChanged(val value: String): ResetPasswordIntent
    data object Submit: ResetPasswordIntent
}

sealed interface ResetPasswordEffect {
    data object PasswordUpdated : ResetPasswordEffect
}
