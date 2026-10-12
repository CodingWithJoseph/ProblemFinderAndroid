package com.example.problemfinder.core.model.auth

enum class AuthFailure {
    INVALID_CREDENTIALS,
    EMAIL_NOT_CONFIRMED,
    ACCOUNT_UNAVAILABLE,
    ACCOUNT_ALREADY_EXISTS,
    REAUTHENTICATION_REQUIRED,
    INVALID_EMAIL,
    INVALID_INPUT,
    WEAK_PASSWORD,
    SAME_PASSWORD,
    RATE_LIMITED,
    EMAIL_RATE_LIMITED,
    SESSION_EXPIRED,
    AUTH_UNAVAILABLE,
    NETWORK,
    UNKNOWN
}

class AuthOperationException(val failure: AuthFailure, cause: Throwable): Exception(cause)
