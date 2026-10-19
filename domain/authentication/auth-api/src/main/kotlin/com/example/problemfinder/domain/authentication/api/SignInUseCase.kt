package com.example.problemfinder.domain.authentication.api

import com.example.problemfinder.core.common.UseCase
import com.example.problemfinder.core.model.auth.AuthUser

/** Authenticates credentials and returns the account identity; failures propagate to the caller. */
abstract class SignInUseCase: UseCase<SignInUseCase.Params, AuthUser>() {
    data class Params(
        val email: String,
        val password: String
    )
}
