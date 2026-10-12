package com.example.problemfinder.feature.authentication.impl.signin

import com.example.problemfinder.core.model.auth.AuthFailure
import com.example.problemfinder.core.model.auth.AuthOperationException
import com.example.problemfinder.core.mvi.BaseMVIViewModel
import com.example.problemfinder.domain.authentication.api.SignInUseCase
import com.example.problemfinder.feature.authentication.impl.AuthUIError
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val signInUseCase: SignInUseCase
) : BaseMVIViewModel<SignInIntent, SignInState, SignInEffect>(SignInState()) {
    override fun onIntent(intent: SignInIntent) {
        when (intent) {
            is SignInIntent.EmailChanged -> reduce {
                copy(email = intent.value, error = null)
            }

            is SignInIntent.PasswordChanged -> reduce {
                copy(password = intent.value, error = null)
            }

            is SignInIntent.Submit -> submit()
            is SignInIntent.SignUpClicked -> sendEffect(SignInEffect.OpenSignUp)
            is SignInIntent.ForgotPasswordClicked -> sendEffect(SignInEffect.OpenForgotPassword)
        }
    }

    private fun submit() = safeLaunch {
        if (state.value.isLoading) return@safeLaunch

        val email = state.value.email.trim()
        val password = state.value.password

        if (email.isBlank() || password.isBlank()) {
            reduce { copy(error = AuthUIError.Validation.EMAIL_AND_PASSWORD_REQUIRED) }
            return@safeLaunch
        }

        reduce { copy(isLoading = true, error = null) }

        try {
            signInUseCase(SignInUseCase.Params(email = email, password = password))
            sendEffect(SignInEffect.SignedIn)
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
