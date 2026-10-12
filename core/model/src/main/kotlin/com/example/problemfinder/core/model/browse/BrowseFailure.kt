package com.example.problemfinder.core.model.browse

enum class BrowseFailure {
    NETWORK,
    SESSION_UNAVAILABLE,
    REQUEST_REJECTED,
    SERVER,
    INVALID_RESPONSE,
    UNKNOWN,
}

class BrowseOperationException(val failure: BrowseFailure, cause: Throwable): Exception(cause)
