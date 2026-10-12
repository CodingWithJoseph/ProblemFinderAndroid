package com.example.problemfinder.domain.authentication.impl

import com.example.problemfinder.domain.authentication.api.ForgotPasswordUseCase
import com.example.problemfinder.domain.authentication.api.ResetPasswordUseCase
import com.example.problemfinder.domain.authentication.api.SignInUseCase
import com.example.problemfinder.domain.authentication.api.SignOutUseCase
import com.example.problemfinder.domain.authentication.api.SignUpUseCase
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent


@Module
@InstallIn(SingletonComponent::class)
abstract class AuthDomainModule {
    @Binds
    abstract fun bindForgotPassword(implementation: ForgotPasswordUseCaseImpl): ForgotPasswordUseCase

    @Binds
    abstract fun bindResetPasswordUseCase(implementation: ResetPasswordUseCaseImpl): ResetPasswordUseCase

    @Binds
    abstract fun bindSignInUseCase(implementation: SignInUseCaseImpl): SignInUseCase

    @Binds
    abstract fun bindSignOutUseCase(implementation: SignOutUseCaseImpl): SignOutUseCase

    @Binds
    abstract fun bindSignUpUseCase(implementation: SignUpUseCaseImpl): SignUpUseCase
}
