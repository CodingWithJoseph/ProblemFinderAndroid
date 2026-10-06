package com.example.problemfinder.app

import androidx.lifecycle.viewModelScope
import com.example.problemfinder.core.mvi.BaseMVIViewModel
import com.example.problemfinder.domain.authentication.api.AuthUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ApplicationViewModel @Inject constructor(
    private val authUseCases: AuthUseCases
): BaseMVIViewModel<ApplicationIntent, ApplicationState, ApplicationEffect>(ApplicationState()) {

    override fun onIntent(intent: ApplicationIntent) {
        when(intent) {
            ApplicationIntent.SignOutClicked -> signOut()
        }
    }

    private fun signOut() {
        if(state.value.isSigningOut) return
        reduce { copy(isSigningOut = true) }

        viewModelScope.launch {
            try {
                authUseCases.signOut()
            } catch (cancelled: CancellationException){
                throw cancelled
            } catch (_: Exception){
                sendEffect(ApplicationEffect.SignOutFailed)
            } finally {
                reduce { copy(isSigningOut = false) }
            }
        }
    }
}
