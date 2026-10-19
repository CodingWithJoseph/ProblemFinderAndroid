package com.example.problemfinder.data.authentication.api
import kotlinx.coroutines.flow.Flow

/** Separates identity used to select cached data from credentials needed for network calls. */
interface AuthSessionProvider {
    /** Emits the known account, including persisted identity during startup or refresh failure. */
    fun observeUserId(): Flow<String?>
    /** Returns initialized, unexpired credentials, or null when no usable session is available. */
    suspend fun currentSession(): AuthSession?
}
