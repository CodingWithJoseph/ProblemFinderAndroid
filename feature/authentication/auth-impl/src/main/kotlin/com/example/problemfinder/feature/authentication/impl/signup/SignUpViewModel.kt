package com.example.problemfinder.feature.authentication.impl.signup

import com.example.problemfinder.core.model.auth.AuthFailure
import com.example.problemfinder.core.model.auth.AuthOperationException
import com.example.problemfinder.core.mvi.BaseMVIViewModel
import com.example.problemfinder.domain.authentication.api.SignUpUseCase
import com.example.problemfinder.feature.authentication.impl.AuthUIError
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val signUpUseCase: SignUpUseCase
) : BaseMVIViewModel<SignUpIntent, SignUpState, SignUpEffect>(SignUpState()) {
    override fun onIntent(intent: SignUpIntent) {
        when (intent) {
            is SignUpIntent.FirstnameChanged -> reduce {
                copy(firstname = intent.value, error = null)
            }

            is SignUpIntent.LastnameChanged -> reduce {
                copy(lastname = intent.value, error = null)
            }

            is SignUpIntent.EmailChanged -> reduce {
                copy(email = intent.value, error = null)
            }

            is SignUpIntent.PasswordChanged -> reduce {
                copy(password = intent.value, error = null)
            }

            is SignUpIntent.ContinueClicked -> continueToProfile()
            is SignUpIntent.PreviousClicked -> {
                if (!state.value.isLoading) reduce { copy(step = SignUpStep.CREDENTIALS, error = null) }
            }
            is SignUpIntent.Submit -> submit()
            is SignUpIntent.SignInClicked -> sendEffect(SignUpEffect.OpenSignIn)
        }
    }

    private fun continueToProfile() {
        if (state.value.isLoading) return

        val email = state.value.email.trim()
        val password = state.value.password
        val error = when {
            email.isBlank() || password.isBlank() -> AuthUIError.Validation.EMAIL_AND_PASSWORD_REQUIRED
            !EMAIL_PATTERN.matches(email) -> AuthUIError.Validation.INVALID_EMAIL
            password.length < 6 -> AuthUIError.Validation.WEAK_PASSWORD
            else -> null
        }

        if (error != null) {
            reduce { copy(error = error) }
        } else {
            reduce { copy(step = SignUpStep.PROFILE, error = null) }
        }
    }

    private fun submit() = safeLaunch {
        if (state.value.isLoading || state.value.step != SignUpStep.PROFILE) return@safeLaunch

        val firstname = state.value.firstname.trim()
        val lastname = state.value.lastname.trim()
        val email = state.value.email.trim()
        val password = state.value.password

        if (firstname.isBlank() || lastname.isBlank()) {
            reduce { copy(error = AuthUIError.Validation.ALL_FIELDS_REQUIRED) }
            return@safeLaunch
        }

        reduce { copy(isLoading = true, error = null) }

        try {
            signUpUseCase(
                SignUpUseCase.Params(
                    firstname = firstname,
                    lastname = lastname,
                    email = email,
                    password = password
                )
            )

            reduce { copy(password = "") }
            sendEffect(SignUpEffect.EmailSent)
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

    private companion object {
        val EMAIL_PATTERN = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    }
}
