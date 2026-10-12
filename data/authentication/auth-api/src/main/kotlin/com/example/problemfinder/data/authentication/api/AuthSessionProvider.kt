package com.example.problemfinder.data.authentication.api
import kotlinx.coroutines.flow.Flow

interface AuthSessionProvider {
    fun observeUserId(): Flow<String?>
    suspend fun currentSession(): AuthSession?
}
