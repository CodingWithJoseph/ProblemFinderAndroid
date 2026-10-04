package com.example.problemfinder.feature.authentication.impl.signin

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
class SignInViewModel @Inject constructor(
    private val authUseCases: AuthUseCases
): BaseMVIViewModel<SignInIntent, SignInState, SignInEffect>(SignInState()){
    override fun onIntent(intent: SignInIntent) {
        when(intent) {
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

    private fun submit() {
        if (state.value.isLoading) return

        val email = state.value.email.trim()
        val password = state.value.password

        if (email.isBlank() || password.isBlank()) {
            reduce { copy(error = AuthUIError.Validation.EMAIL_AND_PASSWORD_REQUIRED) }
            return
        }

        reduce { copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                authUseCases.signIn(email, password)
                sendEffect(SignInEffect.SignedIn)
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
}
