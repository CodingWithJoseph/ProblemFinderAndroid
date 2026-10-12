package com.example.problemfinder.data.authentication.impl

import com.example.problemfinder.core.model.auth.AuthFailure
import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.github.jan.supabase.auth.exception.AuthRestException
import kotlinx.io.IOException

internal object SupabaseAuthErrorMapper {
    private val code = mapOf(
        AuthErrorCode.InvalidCredentials to AuthFailure.INVALID_CREDENTIALS,
        AuthErrorCode.EmailNotConfirmed to AuthFailure.EMAIL_NOT_CONFIRMED,
        AuthErrorCode.UserBanned to AuthFailure.ACCOUNT_UNAVAILABLE,
        AuthErrorCode.EmailExists to AuthFailure.ACCOUNT_ALREADY_EXISTS,
        AuthErrorCode.UserAlreadyExists to AuthFailure.ACCOUNT_ALREADY_EXISTS,
        AuthErrorCode.EmailAddressInvalid to AuthFailure.INVALID_EMAIL,
        AuthErrorCode.ValidationFailed to AuthFailure.INVALID_INPUT,
        AuthErrorCode.WeakPassword to AuthFailure.WEAK_PASSWORD,
        AuthErrorCode.SamePassword to AuthFailure.SAME_PASSWORD,
        AuthErrorCode.OverRequestRateLimit to AuthFailure.RATE_LIMITED,
        AuthErrorCode.OverEmailSendRateLimit to AuthFailure.EMAIL_RATE_LIMITED,
        AuthErrorCode.SessionExpired to AuthFailure.SESSION_EXPIRED,
        AuthErrorCode.SessionNotFound to AuthFailure.SESSION_EXPIRED,
        AuthErrorCode.OtpExpired to AuthFailure.SESSION_EXPIRED,
        AuthErrorCode.ReauthenticationNeeded to AuthFailure.REAUTHENTICATION_REQUIRED,
        AuthErrorCode.SignupDisabled to AuthFailure.AUTH_UNAVAILABLE,
        AuthErrorCode.EmailProviderDisabled to AuthFailure.AUTH_UNAVAILABLE,
        AuthErrorCode.EmailAddressNotAuthorized to AuthFailure.AUTH_UNAVAILABLE,
        AuthErrorCode.RequestTimeout to AuthFailure.NETWORK
    )

    fun map(error: Throwable): AuthFailure = when (error) {
        is AuthRestException -> error.errorCode?.let(code::get) ?: AuthFailure.UNKNOWN
        is IOException -> AuthFailure.NETWORK
        else -> AuthFailure.UNKNOWN
    }
}
