package com.example.problemfinder.domain.authentication.impl

import com.example.problemfinder.core.model.AuthUser
import com.example.problemfinder.data.authentication.api.AuthRepository
import com.example.problemfinder.domain.authentication.api.AuthUseCases
import javax.inject.Inject

class DefaultAuthUseCases @Inject constructor(
    private val repository: AuthRepository
): AuthUseCases {
    override suspend fun signIn(email: String, password: String): AuthUser =
        repository.signIn(email = email, password = password)

    override suspend fun createAccount(
        firstname: String,
        lastname: String,
        email: String,
        password: String
    ) = repository.createAccount(firstname, lastname, email, password)

    override suspend fun forgotPassword(email: String) {
        repository.forgotPassword(email)
    }

    override suspend fun resetPassword(password: String) {
        repository.resetPassword(password)
    }

    override suspend fun signOut() {
        repository.signOut()
    }


}
