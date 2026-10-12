package com.example.problemfinder.domain.authentication.impl

import com.example.problemfinder.data.authentication.api.AuthRepository
import com.example.problemfinder.domain.authentication.api.ResetPasswordUseCase
import javax.inject.Inject

class ResetPasswordUseCaseImpl @Inject constructor(
    private val authRepository: AuthRepository
): ResetPasswordUseCase() {
    override suspend fun execute(params: ResetPasswordUseCase.Params) = authRepository.resetPassword(params.password)
}
