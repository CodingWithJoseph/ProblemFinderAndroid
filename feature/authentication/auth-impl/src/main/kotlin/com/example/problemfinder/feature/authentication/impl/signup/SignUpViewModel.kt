package com.example.problemfinder.feature.authentication.impl.signup

import androidx.lifecycle.viewModelScope
import com.example.problemfinder.core.model.AuthFailure
import com.example.problemfinder.core.model.AuthOperationException
import com.example.problemfinder.core.mvi.BaseMVIViewModel
import com.example.problemfinder.domain.authentication.api.AuthUseCases
import com.example.problemfinder.feature.authentication.impl.AuthUIError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val authUseCases: AuthUseCases
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

    private fun submit() {
        if (state.value.isLoading || state.value.step != SignUpStep.PROFILE) return

        val firstname = state.value.firstname.trim()
        val lastname = state.value.lastname.trim()
        val email = state.value.email.trim()
        val password = state.value.password

        if (firstname.isBlank() || lastname.isBlank()) {
            reduce { copy(error = AuthUIError.Validation.ALL_FIELDS_REQUIRED) }
            return
        }

        reduce { copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                authUseCases.createAccount(
                    firstname = firstname,
                    lastname = lastname,
                    email = email,
                    password = password
                )

                reduce { copy(password = "") }
                sendEffect(SignUpEffect.EmailSent)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: AuthOperationException) {
                reduce { copy(error = AuthUIError.Service(exception.failure)) }
            } catch (_: Exception) {
                reduce { copy(error = AuthUIError.Service(AuthFailure.UNKNOWN)) }
            } finally {
                reduce { copy(isLoading = false) }
            }
        }
    }

    private companion object {
        val EMAIL_PATTERN = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    }
}
