package com.example.problemfinder.feature.browse.impl

import com.example.problemfinder.core.model.browse.BrowseFailure

data class BrowseUIError(
    val failure: BrowseFailure
)

private val serviceMessage = mapOf(
    BrowseFailure.NETWORK to (R.string.title_error_network to R.string.message_error_network),
    BrowseFailure.SESSION_UNAVAILABLE to (R.string.title_failure to R.string.message_error_session),
    BrowseFailure.REQUEST_REJECTED to (R.string.title_failure to R.string.message_error_request),
    BrowseFailure.SERVER to (R.string.title_failure to R.string.message_error_server),
    BrowseFailure.INVALID_RESPONSE to (R.string.title_failure to R.string.message_error_response),
    BrowseFailure.UNKNOWN to (R.string.title_failure to R.string.message_failure),
)

internal fun BrowseUIError.messageRes(): Pair<Int, Int> = serviceMessage[this.failure] ?:
    Pair(R.string.title_failure, R.string.message_failure)
