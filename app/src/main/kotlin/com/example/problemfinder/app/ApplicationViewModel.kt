package com.example.problemfinder.app

import com.example.problemfinder.core.mvi.BaseMVIViewModel
import com.example.problemfinder.core.mvi.RequestState
import com.example.problemfinder.data.authentication.api.AuthSessionProvider
import com.example.problemfinder.domain.authentication.api.SignOutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/** Observes account identity for root navigation and runs sign-out requests for the app shell. */
@HiltViewModel
class ApplicationViewModel @Inject constructor(
    private val signOutUseCase: SignOutUseCase,
    private val sessions: AuthSessionProvider,
) : BaseMVIViewModel<ApplicationIntent, ApplicationState, ApplicationEffect>(ApplicationState()) {

    private var accountJob: Job? = null
    private var accountVersion = 0L

    init {
        observeAccount()
    }

    override fun onIntent(intent: ApplicationIntent) {
        when (intent) {
            ApplicationIntent.SignOutClicked -> signOut()
            ApplicationIntent.RetryAccountRead -> observeAccount()
        }
    }

    /** Restarts identity observation; failures become retryable startup state, not sign-out effects. */
    private fun observeAccount() {
        // Cancellation stops the old collector; the version also rejects any late result from it.
        accountVersion += 1
        val version = accountVersion
        accountJob?.cancel()
        reduce { copy(accountRequest = RequestState.Loading) }

        accountJob = safeLaunch {
            try {
                sessions.observeUserId().collect { userId ->
                    if (version == accountVersion) {
                        reduce { copy(accountRequest = RequestState.Success(userId)) }
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (version == accountVersion) {
                    reduce { copy(accountRequest = RequestState.Error(Unit)) }
                }
            }
        }
    }

    // The session provider owns the eventual account change. Do not clear identity optimistically:
    // a failed sign-out must leave the current account available and offer a retry.
    private fun signOut() = safeLaunch {
        if (state.value.isSigningOut) return@safeLaunch
        try {
            reduce { copy(isSigningOut = true) }
            signOutUseCase(Unit)
        } finally {
            reduce { copy(isSigningOut = false) }
        }
    }

    override fun onError(error: Throwable) {
        sendEffect(ApplicationEffect.SignOutFailed)
    }
}
