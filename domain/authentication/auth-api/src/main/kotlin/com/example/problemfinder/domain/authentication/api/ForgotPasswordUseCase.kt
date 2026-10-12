package com.example.problemfinder.domain.authentication.api

import com.example.problemfinder.core.common.UseCase

abstract class ForgotPasswordUseCase: UseCase<ForgotPasswordUseCase.Params, Unit>() {
    data class Params(val email: String)
}
