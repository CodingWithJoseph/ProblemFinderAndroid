package com.example.problemfinder.domain.authentication.impl

import com.example.problemfinder.data.authentication.api.AuthRepository
import com.example.problemfinder.domain.authentication.api.ForgotPasswordUseCase
import javax.inject.Inject

class ForgotPasswordUseCaseImpl @Inject constructor(
    private val authRepository: AuthRepository
): ForgotPasswordUseCase() {
    override suspend fun execute(params: Params) = authRepository.forgotPassword(params.email)
}
