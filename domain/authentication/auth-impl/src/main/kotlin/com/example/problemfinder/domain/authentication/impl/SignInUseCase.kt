package com.example.problemfinder.domain.authentication.impl

import com.example.problemfinder.data.authentication.api.AuthRepository
import com.example.problemfinder.domain.authentication.api.SignInUseCase
import javax.inject.Inject

class SignInUseCaseImpl @Inject constructor(
    private val authRepository: AuthRepository
) : SignInUseCase() {
    override suspend fun execute(params: Params) = authRepository.signIn(params.email, params.password)
}
