package com.example.problemfinder.domain.authentication.api

import com.example.problemfinder.core.common.UseCase

abstract class SignUpUseCase: UseCase<SignUpUseCase.Params, Unit>() {
    data class Params(
        val firstname: String,
        val lastname: String,
        val email: String,
        val password: String
    )
}
