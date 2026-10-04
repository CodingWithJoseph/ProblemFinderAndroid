package com.example.problemfinder.feature.authentication.impl.forgotpassword

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
class ForgotPasswordViewModel @Inject constructor(
    private val authUseCases: AuthUseCases
) : BaseMVIViewModel<ForgotPasswordIntent, ForgotPasswordState, ForgotPasswordEffect>(ForgotPasswordState()) {
    override fun onIntent(intent: ForgotPasswordIntent) {
        when (intent) {
            is ForgotPasswordIntent.EmailChanged -> reduce {
                copy(email = intent.value, error = null)
            }

            is ForgotPasswordIntent.Submit -> submit()
            is ForgotPasswordIntent.SignInClicked -> sendEffect(ForgotPasswordEffect.OpenSignIn)
        }
    }

    private fun submit() {
        if (state.value.isLoading) return

        val email = state.value.email.trim()

        if (email.isBlank()) {
            reduce { copy(error = AuthUIError.Validation.EMAIL_REQUIRED) }
            return
        }

        reduce { copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                authUseCases.forgotPassword(email = email)
                sendEffect(ForgotPasswordEffect.EmailSent)
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
