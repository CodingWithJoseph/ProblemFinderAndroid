package com.example.problemfinder.domain.authentication.impl

import com.example.problemfinder.data.authentication.api.AuthRepository
import com.example.problemfinder.domain.authentication.api.SignUpUseCase
import javax.inject.Inject

class SignUpUseCaseImpl @Inject constructor(
    private val authRepository: AuthRepository
): SignUpUseCase() {
    override suspend fun execute(params: Params) = authRepository.createAccount(
        firstname = params.firstname,
        lastname = params.lastname,
        email = params.email,
        password = params.password
    )
}
