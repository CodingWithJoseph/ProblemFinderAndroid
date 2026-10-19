package com.example.problemfinder.domain.authentication.api

import com.example.problemfinder.core.common.UseCase

/** Changes the current session's password after the recovery link establishes that session. */
abstract class ResetPasswordUseCase: UseCase<ResetPasswordUseCase.Params, Unit>() {
    data class Params(val password: String)
}
