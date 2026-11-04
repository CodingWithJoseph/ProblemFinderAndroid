package com.example.problemfinder.data.opportunities.impl.util

import com.example.problemfinder.core.model.opportunity.OpportunityFailure
import com.example.problemfinder.core.model.opportunity.OpportunityOperationException
import com.example.problemfinder.data.authentication.api.AuthSession
import com.example.problemfinder.data.authentication.api.AuthSessionProvider
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.serialization.ContentConvertException
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerializationException
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException


/** Obtains credentials only for network work; observing an existing cache does not need a token. */
internal suspend fun requireSession(sessions: AuthSessionProvider): AuthSession =
    sessions.currentSession() ?: throw OpportunityOperationException(
        OpportunityFailure.SESSION_UNAVAILABLE,
        IllegalStateException("No usable opportunity session"),
    )

/**
 * Rejects work for an account no longer active at this checkpoint. Compare user IDs, not access
 * tokens: a token refresh for the same account does not make its response stale.
 */
internal suspend fun ensureSameAccount(userId: String, sessions: AuthSessionProvider) {
    if (sessions.observeUserId().first() != userId) {
        throw CancellationException("Opportunity account changed")
    }
}

/**
 * Converts infrastructure failures into the feature's stable failure categories. Cancellation and
 * already-classified failures pass through unchanged so account switches do not become error UI.
 */
internal suspend fun <T> opportunityCall(block: suspend () -> T): T =
    try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: OpportunityOperationException) {
        throw error
    } catch (error: Exception) {
        val failure = when (error) {
            is ResponseException -> when (error.response.status.value) {
                401 -> OpportunityFailure.SESSION_UNAVAILABLE
                404 -> OpportunityFailure.CLUSTER_NOT_FOUND
                in 400..499 -> OpportunityFailure.REQUEST_REJECTED
                in 500..599 -> OpportunityFailure.SERVER
                else -> OpportunityFailure.UNKNOWN
            }

            is ContentConvertException,
            is SerializationException -> OpportunityFailure.INVALID_RESPONSE

            is HttpRequestTimeoutException,
            is IOException -> OpportunityFailure.NETWORK

            else -> OpportunityFailure.UNKNOWN
        }

        throw OpportunityOperationException(failure, error)
    }
