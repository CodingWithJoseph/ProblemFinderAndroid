package com.example.problemfinder.feature.authentication.impl.resetpassword

import com.example.problemfinder.core.model.auth.AuthFailure
import com.example.problemfinder.core.model.auth.AuthOperationException
import com.example.problemfinder.core.mvi.BaseMVIViewModel
import com.example.problemfinder.domain.authentication.api.ResetPasswordUseCase
import com.example.problemfinder.feature.authentication.impl.AuthUIError
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ResetPasswordViewModel @Inject constructor(
    private val resetPasswordUseCase: ResetPasswordUseCase
) : BaseMVIViewModel<ResetPasswordIntent, ResetPasswordState, ResetPasswordEffect>(ResetPasswordState()) {
    override fun onIntent(intent: ResetPasswordIntent) {
        when (intent) {
            is ResetPasswordIntent.PasswordChanged -> reduce {
                copy(password = intent.value, error = null)
            }

            is ResetPasswordIntent.ConfirmationPasswordChanged -> reduce {
                copy(confirmationPassword = intent.value, error = null)
            }

            is ResetPasswordIntent.Submit -> submit()
        }
    }

    private fun submit() = safeLaunch {
        if (state.value.isLoading) return@safeLaunch

        val password = state.value.password
        val confirmationPassword = state.value.confirmationPassword

        if (password.isBlank() || confirmationPassword.isBlank()) {
            reduce { copy(error = AuthUIError.Validation.NEW_PASSWORD_REQUIRED) }
            return@safeLaunch
        }

        if (password != confirmationPassword){
            reduce { copy(error = AuthUIError.Validation.PASSWORD_MISMATCH) }
            return@safeLaunch
        }

        reduce { copy(isLoading = true, error = null) }

        try {
            resetPasswordUseCase(ResetPasswordUseCase.Params(password=password))
            sendEffect(ResetPasswordEffect.PasswordUpdated)
        } finally {
            reduce { copy(isLoading = false) }
        }
    }

    override fun onError(error: Throwable) {
        when (error) {
            is AuthOperationException -> handleAuthException(error)
            else -> handleUnknownException()
        }
    }

    private fun handleAuthException(e: AuthOperationException) = reduce {
        copy(error = AuthUIError.Service(e.failure))
    }

    private fun handleUnknownException() = reduce {
        copy(error = AuthUIError.Service(AuthFailure.UNKNOWN))
    }
}
