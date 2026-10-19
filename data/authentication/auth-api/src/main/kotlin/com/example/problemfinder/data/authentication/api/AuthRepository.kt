package com.example.problemfinder.data.authentication.api

import com.example.problemfinder.core.model.auth.AuthUser

/**
 * Authentication commands used by domain use cases. Session observation is separate in
 * [AuthSessionProvider]; implementations report service failures as AuthOperationException.
 */
interface AuthRepository {
    suspend fun signIn(email: String, password: String): AuthUser
    /** Creates the account without promising that email confirmation or sign-in is complete. */
    suspend fun createAccount(firstname: String, lastname: String, email: String, password: String)
    suspend fun forgotPassword(email: String)
    /** Updates the password of the current session, including a session restored by a recovery link. */
    suspend fun resetPassword(password: String)
    suspend fun signOut()
}
