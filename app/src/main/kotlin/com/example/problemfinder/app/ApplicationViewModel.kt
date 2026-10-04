package com.example.problemfinder.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.problemfinder.domain.authentication.api.AuthUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ApplicationViewModel @Inject constructor(
    private val authUseCases: AuthUseCases
): ViewModel() {
    var isSigningOut by mutableStateOf(false)
        private set

    var signOutFailed by mutableStateOf(false)
        private set

    fun signOut() {
        if(isSigningOut) return

        isSigningOut = true
        signOutFailed = false

        viewModelScope.launch {
            try {
                authUseCases.signOut()
            } catch (cancelled: CancellationException){
                throw cancelled
            } catch (_: Exception){
                signOutFailed = true
            } finally {
                isSigningOut = false
            }
        }
    }
}
