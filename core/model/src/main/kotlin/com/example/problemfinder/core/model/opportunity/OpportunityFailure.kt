package com.example.problemfinder.core.model.opportunity

/** Stable failure categories shared by repositories and UI without exposing HTTP/Room types. */
enum class OpportunityFailure {
    NETWORK,
    SESSION_UNAVAILABLE,
    REQUEST_REJECTED,
    SERVER,
    CLUSTER_NOT_FOUND,
    INVALID_RESPONSE,
    UNKNOWN,
}

class OpportunityOperationException(val failure: OpportunityFailure, cause: Throwable): Exception(cause)
