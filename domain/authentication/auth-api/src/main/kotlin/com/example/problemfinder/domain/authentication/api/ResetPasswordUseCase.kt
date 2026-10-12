package com.example.problemfinder.domain.authentication.api

import com.example.problemfinder.core.common.UseCase

abstract class ResetPasswordUseCase: UseCase<ResetPasswordUseCase.Params, Unit>() {
    data class Params(val password: String)
}
