package com.example.problemfinder.data.authentication.impl

import com.example.problemfinder.data.authentication.api.AuthSession
import com.example.problemfinder.data.authentication.api.AuthSessionProvider
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.NoSessionFoundException
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import javax.inject.Inject
import kotlin.time.Clock

class SupabaseAuthSessionProvider @Inject constructor(
    private val client: SupabaseClient
) : AuthSessionProvider {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeUserId(): Flow<String?> =
        client.auth.sessionStatus
            .mapLatest { status ->
                when (status) {
                    is SessionStatus.Authenticated -> status.session.user?.id
                    is SessionStatus.NotAuthenticated -> null
                    SessionStatus.Initializing,
                    is SessionStatus.RefreshFailure -> persistedUserId()
                }
            }.distinctUntilChanged()

    override suspend fun currentSession(): AuthSession? {
        client.auth.awaitInitialization()

        val session = client.auth.currentSessionOrNull() ?: return null
        val userId = session.user?.id ?: return null

        if(session.expiresAt <= Clock.System.now()) return null

        return AuthSession(
            userId = userId,
            accessToken = session.accessToken
        )
    }


    private suspend fun persistedUserId(): String? =
        try {
            client.auth.sessionManager.loadSession().user?.id
        } catch (_: NoSessionFoundException) {
            null
        }

}
