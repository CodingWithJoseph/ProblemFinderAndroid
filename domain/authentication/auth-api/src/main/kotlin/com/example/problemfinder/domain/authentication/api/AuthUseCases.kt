package com.example.problemfinder.domain.authentication.api

import com.example.problemfinder.core.model.AuthUser

interface AuthUseCases {
    suspend fun signIn(email:String, password: String): AuthUser
    suspend fun createAccount(firstname: String, lastname: String, email: String, password: String)
    suspend fun forgotPassword(email: String)
    suspend fun resetPassword(password: String)
    suspend fun signOut()
}
