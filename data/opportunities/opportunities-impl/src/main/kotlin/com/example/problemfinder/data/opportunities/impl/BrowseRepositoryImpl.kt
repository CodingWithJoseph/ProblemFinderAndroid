package com.example.problemfinder.data.opportunities.impl

import com.example.problemfinder.core.common.shared.OpportunityCategoryFilter
import com.example.problemfinder.core.common.shared.SortType
import com.example.problemfinder.core.model.opportunity.OpportunityFailure
import com.example.problemfinder.core.model.opportunity.OpportunityOperationException
import com.example.problemfinder.core.model.browse.BrowsePage
import com.example.problemfinder.core.model.browse.BrowseSnapshot
import com.example.problemfinder.data.authentication.api.AuthSession
import com.example.problemfinder.data.authentication.api.AuthSessionProvider
import com.example.problemfinder.data.opportunities.api.BrowseRepository
import com.example.problemfinder.data.opportunities.impl.local.BrowseLocalDataSource
import com.example.problemfinder.data.opportunities.impl.remote.BrowseRemoteDataSource
import com.example.problemfinder.data.opportunities.impl.util.opportunityCall
import com.example.problemfinder.data.opportunities.impl.util.ensureSameAccount
import com.example.problemfinder.data.opportunities.impl.util.requireSession
import com.example.problemfinder.data.opportunities.impl.util.toModel
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.serialization.ContentConvertException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import com.example.problemfinder.data.opportunities.impl.util.OpportunitySyncCoordinator
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

class BrowseRepositoryImpl @Inject constructor(
    private val sessions: AuthSessionProvider,
    private val remote: BrowseRemoteDataSource,
    private val local: BrowseLocalDataSource,
    private val sync: OpportunitySyncCoordinator,
) : BrowseRepository {


    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeBrowse(
        category: OpportunityCategoryFilter,
        sortType: SortType,
    ): Flow<BrowseSnapshot> =
        sessions.observeUserId().flatMapLatest { userId ->
            if (userId == null) {
                flowOf(BrowseSnapshot())
            } else {
                local.observeBrowse(userId, category, sortType)
                    .map { it.copy(userId = userId) }
                    .onStart {
                        emit(BrowseSnapshot(userId = userId, cacheReady = false))
                    }
            }
        }

    override suspend fun refresh(
        category: OpportunityCategoryFilter,
        sortType: SortType,
    ): Unit = opportunityCall {
        val session = requireSession(sessions)

        sync.mutex.withLock {
            ensureSameAccount(session.userId, sessions)
            val page = fetchPage(session, category, sortType, cursor = null)
            ensureSameAccount(session.userId, sessions)
            local.replaceFirstPage(session.userId, category, sortType, page)
        }
    }

    override suspend fun loadNextPage(
        category: OpportunityCategoryFilter,
        sortType: SortType,
    ): Unit = opportunityCall {
        val session = requireSession(sessions)
        val query = local.getQuery(session.userId, category, sortType)
            ?: return@opportunityCall
        if (!query.hasSynced) return@opportunityCall
        val expectedCursor = query.nextCursor ?: return@opportunityCall

        sync.mutex.withLock {
            ensureSameAccount(session.userId, sessions)
            val current = local.getQuery(session.userId, category, sortType)

            if (current?.hasSynced != true ||
                current.nextCursor != expectedCursor
            ) {
                return@withLock
            }

            val page = fetchPage(session, category, sortType, expectedCursor)

            if (page.nextCursor == expectedCursor) {
                throw OpportunityOperationException(
                    OpportunityFailure.INVALID_RESPONSE,
                    IllegalStateException("Browse cursor did not advance"),
                )
            }

            ensureSameAccount(session.userId, sessions)
            local.appendPage(
                userId = session.userId,
                category = category,
                sortType = sortType,
                expectedCursor = expectedCursor,
                page = page,
            )
        }
    }

    private suspend fun fetchPage(
        session: AuthSession,
        category: OpportunityCategoryFilter,
        sort: SortType,
        cursor: String?,
    ): BrowsePage {
        val dto = remote.fetchBrowsePage(
            category = category,
            sortType = sort,
            accessToken = session.accessToken,
            cursor = cursor,
        )

        return try {
            dto.toModel()
        } catch (error: IllegalArgumentException) {
            throw OpportunityOperationException(OpportunityFailure.INVALID_RESPONSE, error)
        }
    }
}
