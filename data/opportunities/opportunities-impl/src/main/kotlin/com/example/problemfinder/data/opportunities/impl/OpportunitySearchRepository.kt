package com.example.problemfinder.data.opportunities.impl

import com.example.problemfinder.core.model.opportunity.OpportunityFailure
import com.example.problemfinder.core.model.opportunity.OpportunityOperationException
import com.example.problemfinder.core.model.search.*
import com.example.problemfinder.data.authentication.api.AuthSessionProvider
import com.example.problemfinder.data.opportunities.api.OpportunitySearchRepository
import com.example.problemfinder.data.opportunities.impl.local.OpportunitySearchLocalDataSource
import com.example.problemfinder.data.opportunities.impl.model.dto.OpportunitySearchInterpretRequestDto
import com.example.problemfinder.data.opportunities.impl.remote.OpportunitySearchRemoteDataSource
import com.example.problemfinder.data.opportunities.impl.util.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

class OpportunitySearchRepositoryImpl @Inject constructor(
    private val sessions: AuthSessionProvider,
    private val remote: OpportunitySearchRemoteDataSource,
    private val local: OpportunitySearchLocalDataSource,
    private val sync: OpportunitySyncCoordinator,
) : OpportunitySearchRepository {
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeSearch(query: OpportunitySearchQuery): Flow<OpportunitySearchSnapshot> {
        val normalized = query.normalized()
        return sessions.observeUserId().distinctUntilChanged().flatMapLatest { userId ->
            val initial = OpportunitySearchSnapshot(normalized, userId, cacheReady = userId == null)
            if (userId == null) flowOf(initial)
            else local.observeSearch(userId, normalized).onStart { emit(initial) }
        }
    }

    override suspend fun refreshSearch(query: OpportunitySearchQuery) = fetch(query, replace = true)

    override suspend fun loadNextSearchPage(query: OpportunitySearchQuery) = fetch(query, replace = false)

    private suspend fun fetch(query: OpportunitySearchQuery, replace: Boolean): Unit = opportunityCall {
        val normalized = query.normalized()
        val session = requireSession(sessions)
        sync.mutex.withLock {
            ensureSameAccount(session.userId, sessions)
            val offset = if (replace) 0 else local.nextOffset(session.userId, normalized) ?: return@withLock
            val request = normalized.toSearchRequest(offset)
            val response = remote.fetchResults(request, session.accessToken)
            val page = validateResponse { response.toSearchPage(request) }
            ensureSameAccount(session.userId, sessions)
            local.storePage(session.userId, normalized, page, replace)
        }
    }

    override suspend fun prepareDraft(message: String, currentDraft: OpportunitySearchQuery?) = opportunityCall {
        val text = message.trim()
        require(text.length in 2..2_000)
        val session = requireSession(sessions)
        val response = remote.prepareDraft(
            OpportunitySearchInterpretRequestDto(text, currentDraft?.toSearchRequest(0)), session.accessToken,
        )
        ensureSameAccount(session.userId, sessions)
        validateResponse { response.toSearchDraft() }
    }

    override suspend fun filterOptions() = opportunityCall {
        val session = requireSession(sessions)
        val options = remote.options(session.accessToken)
        ensureSameAccount(session.userId, sessions)
        OpportunitySearchOptions(options.sources, options.communities)
    }

    private inline fun <T> validateResponse(convert: () -> T): T = try {
        convert()
    } catch (error: IllegalArgumentException) {
        throw OpportunityOperationException(OpportunityFailure.INVALID_RESPONSE, error)
    }
}
