package com.example.problemfinder.feature.opportunities.impl.util

import com.example.problemfinder.core.model.opportunity.OpportunityFailure
import com.example.problemfinder.feature.opportunities.impl.R

/** Presentation-safe failure category; raw exception details are not shown to the user. */
data class OpportunityUiError(
    val failure: OpportunityFailure
)

private val serviceMessage = mapOf(
    OpportunityFailure.NETWORK to (R.string.title_error_network to R.string.message_error_network),
    OpportunityFailure.SESSION_UNAVAILABLE to (R.string.title_failure to R.string.message_error_session),
    OpportunityFailure.REQUEST_REJECTED to (R.string.title_failure to R.string.message_error_request),
    OpportunityFailure.SERVER to (R.string.title_failure to R.string.message_error_server),
    OpportunityFailure.CLUSTER_NOT_FOUND to (R.string.title_cluster_not_found to R.string.message_cluster_not_found),
    OpportunityFailure.INVALID_RESPONSE to (R.string.title_failure to R.string.message_error_response),
    OpportunityFailure.UNKNOWN to (R.string.title_failure to R.string.message_failure),
)

/** Returns title then message resource IDs; composables/hosts resolve them with the current locale. */
internal fun OpportunityUiError.messageRes(): Pair<Int, Int> = serviceMessage[this.failure] ?:
    Pair(R.string.title_failure, R.string.message_failure)
