package com.example.problemfinder.domain.authentication.impl

import com.example.problemfinder.data.authentication.api.AuthRepository
import com.example.problemfinder.domain.authentication.api.SignOutUseCase
import javax.inject.Inject


class SignOutUseCaseImpl @Inject constructor(
    private val authRepository: AuthRepository
): SignOutUseCase() {
    override suspend fun execute(params: Unit) = authRepository.signOut()
}
