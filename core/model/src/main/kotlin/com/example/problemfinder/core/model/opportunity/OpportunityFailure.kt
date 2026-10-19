package com.example.problemfinder.core.model.opportunity

enum class OpportunityFailure {
    NETWORK,
    SESSION_UNAVAILABLE,
    REQUEST_REJECTED,
    SERVER,
    INVALID_RESPONSE,
    UNKNOWN,
}

class OpportunityOperationException(val failure: OpportunityFailure, cause: Throwable): Exception(cause)
