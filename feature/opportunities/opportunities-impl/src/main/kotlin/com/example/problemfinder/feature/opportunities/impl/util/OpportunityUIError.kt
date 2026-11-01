package com.example.problemfinder.feature.opportunities.impl.util

import com.example.problemfinder.core.model.opportunity.OpportunityFailure
import com.example.problemfinder.core.designsystem.R as DesignSystemR

/** Presentation-safe failure category; raw exception details are not shown to the user. */
data class OpportunityUIError(
    val failure: OpportunityFailure
)

private val serviceMessage = mapOf(
    OpportunityFailure.NETWORK to (DesignSystemR.string.title_error_network to DesignSystemR.string.message_error_network),
    OpportunityFailure.SESSION_UNAVAILABLE to (DesignSystemR.string.title_failure to DesignSystemR.string.message_error_session),
    OpportunityFailure.REQUEST_REJECTED to (DesignSystemR.string.title_failure to DesignSystemR.string.message_error_request),
    OpportunityFailure.SERVER to (DesignSystemR.string.title_failure to DesignSystemR.string.message_error_server),
    OpportunityFailure.CLUSTER_NOT_FOUND to (DesignSystemR.string.title_cluster_not_found to DesignSystemR.string.message_cluster_not_found),
    OpportunityFailure.INVALID_RESPONSE to (DesignSystemR.string.title_failure to DesignSystemR.string.message_error_response),
    OpportunityFailure.UNKNOWN to (DesignSystemR.string.title_failure to DesignSystemR.string.message_failure),
)

/** Returns title then message resource IDs; composables/hosts resolve them with the current locale. */
internal fun OpportunityUIError.messageRes(): Pair<Int, Int> = serviceMessage[this.failure] ?:
    Pair(DesignSystemR.string.title_failure, DesignSystemR.string.message_failure)
