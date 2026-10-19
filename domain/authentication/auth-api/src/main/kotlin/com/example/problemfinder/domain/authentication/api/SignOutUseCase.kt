package com.example.problemfinder.domain.authentication.api

import com.example.problemfinder.core.common.UseCase


/** Requests sign-out; callers can retain the current screen and offer retry when it fails. */
abstract class SignOutUseCase: UseCase<Unit, Unit>()
