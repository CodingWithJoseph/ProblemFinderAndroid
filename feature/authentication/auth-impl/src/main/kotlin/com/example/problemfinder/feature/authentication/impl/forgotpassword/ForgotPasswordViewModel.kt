package com.example.problemfinder.feature.authentication.impl.forgotpassword

import com.example.problemfinder.core.model.auth.AuthFailure
import com.example.problemfinder.core.model.auth.AuthOperationException
import com.example.problemfinder.core.mvi.BaseMVIViewModel
import com.example.problemfinder.domain.authentication.api.ForgotPasswordUseCase
import com.example.problemfinder.feature.authentication.impl.AuthUIError
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val forgotPasswordUseCase: ForgotPasswordUseCase
) : BaseMVIViewModel<ForgotPasswordIntent, ForgotPasswordState, ForgotPasswordEffect>(
    ForgotPasswordState()
) {
    override fun onIntent(intent: ForgotPasswordIntent) {
        when (intent) {
            is ForgotPasswordIntent.EmailChanged -> reduce {
                copy(email = intent.value, error = null)
            }

            is ForgotPasswordIntent.Submit -> submit()
            is ForgotPasswordIntent.SignInClicked -> sendEffect(ForgotPasswordEffect.OpenSignIn)
        }
    }

    private fun submit() = safeLaunch {
        if (state.value.isLoading) return@safeLaunch

        val email = state.value.email.trim()

        if (email.isBlank()) {
            reduce { copy(error = AuthUIError.Validation.EMAIL_REQUIRED) }
            return@safeLaunch
        }

        reduce { copy(isLoading = true, error = null) }

        try {
            forgotPasswordUseCase(ForgotPasswordUseCase.Params(email))
            sendEffect(ForgotPasswordEffect.EmailSent)
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
