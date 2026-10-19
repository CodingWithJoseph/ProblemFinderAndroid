package com.example.problemfinder.domain.authentication.api

import com.example.problemfinder.core.common.UseCase

/** Requests a recovery email; completion means the request succeeded, not that a link was opened. */
abstract class ForgotPasswordUseCase: UseCase<ForgotPasswordUseCase.Params, Unit>() {
    data class Params(val email: String)
}
