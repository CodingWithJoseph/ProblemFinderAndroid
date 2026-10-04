package com.example.problemfinder.feature.authentication.impl.resetpassword

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
class ResetPasswordViewModel @Inject constructor(
    private val authUseCases: AuthUseCases
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

    private fun submit() {
        if (state.value.isLoading) return

        val password = state.value.password
        val confirmationPassword = state.value.confirmationPassword

        if (password.isBlank() || confirmationPassword.isBlank()) {
            reduce { copy(error = AuthUIError.Validation.NEW_PASSWORD_REQUIRED) }
            return
        }

        if (password != confirmationPassword){
            reduce { copy(error = AuthUIError.Validation.PASSWORD_MISMATCH) }
            return
        }

        reduce { copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                authUseCases.resetPassword(password = password)
                sendEffect(ResetPasswordEffect.PasswordUpdated)
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
