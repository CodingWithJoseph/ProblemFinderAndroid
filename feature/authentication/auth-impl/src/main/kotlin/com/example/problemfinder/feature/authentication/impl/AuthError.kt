package com.example.problemfinder.feature.authentication.impl

import androidx.annotation.StringRes
import com.example.problemfinder.core.model.auth.AuthFailure

/** Keeps local form validation distinct from service failures until the UI selects localized text. */
sealed interface AuthUIError {
    data class Service(val failure: AuthFailure): AuthUIError
    enum class Validation: AuthUIError {
        EMAIL_AND_PASSWORD_REQUIRED,
        INVALID_EMAIL,
        WEAK_PASSWORD,
        ALL_FIELDS_REQUIRED,
        EMAIL_REQUIRED,
        NEW_PASSWORD_REQUIRED,
        PASSWORD_MISMATCH
    }
}

internal enum class AuthOperation(@StringRes val fallbackRes: Int) {
    SIGN_IN(R.string.auth_error_sign_in_unknown),
    SIGN_UP(R.string.auth_error_sign_up_unknown),
    FORGOT_PASSWORD(R.string.auth_error_reset_email_unknown),
    RESET_PASSWORD(R.string.auth_error_reset_password_unknown)
}

private val serviceMessages = mapOf(
    AuthFailure.INVALID_CREDENTIALS to R.string.auth_error_invalid_credentials,
    AuthFailure.EMAIL_NOT_CONFIRMED to R.string.auth_error_email_not_confirmed,
    AuthFailure.ACCOUNT_UNAVAILABLE to R.string.auth_error_account_unavailable,
    AuthFailure.ACCOUNT_ALREADY_EXISTS to R.string.auth_error_account_already_exists,
    AuthFailure.REAUTHENTICATION_REQUIRED to R.string.auth_error_reauthentication_required,
    AuthFailure.INVALID_EMAIL to R.string.auth_error_invalid_email,
    AuthFailure.INVALID_INPUT to R.string.auth_error_invalid_input,
    AuthFailure.WEAK_PASSWORD to R.string.auth_error_invalid_password,
    AuthFailure.SAME_PASSWORD to R.string.auth_error_same_password,
    AuthFailure.RATE_LIMITED to R.string.auth_error_rate_limited,
    AuthFailure.EMAIL_RATE_LIMITED to R.string.auth_error_email_rate_limited,
    AuthFailure.SESSION_EXPIRED to R.string.auth_error_session_expired,
    AuthFailure.AUTH_UNAVAILABLE to R.string.auth_error_auth_unavailable,
    AuthFailure.NETWORK to R.string.auth_error_network
)

private val validationMessages = mapOf(
    AuthUIError.Validation.EMAIL_AND_PASSWORD_REQUIRED to R.string.auth_error_email_password_required,
    AuthUIError.Validation.INVALID_EMAIL to R.string.auth_error_invalid_email,
    AuthUIError.Validation.WEAK_PASSWORD to R.string.auth_error_invalid_password,
    AuthUIError.Validation.ALL_FIELDS_REQUIRED to R.string.auth_error_all_fields_required,
    AuthUIError.Validation.EMAIL_REQUIRED to R.string.auth_error_email_required,
    AuthUIError.Validation.NEW_PASSWORD_REQUIRED to R.string.auth_error_new_password_required,
    AuthUIError.Validation.PASSWORD_MISMATCH to R.string.auth_error_password_mismatch
)

/** Uses operation-specific fallback text and explains an expired recovery session as an expired link. */
@StringRes
internal fun AuthUIError.messageRes(operation: AuthOperation): Int = when(this) {
    is AuthUIError.Service -> {
        if (failure == AuthFailure.SESSION_EXPIRED && operation == AuthOperation.RESET_PASSWORD){
            R.string.auth_error_reset_link_expired
        } else {
            serviceMessages[failure] ?: operation.fallbackRes
        }
    }

    is AuthUIError.Validation -> validationMessages.getValue(this)
}
