package com.example.problemfinder.data.browse.impl

import com.example.problemfinder.core.common.shared.Category
import com.example.problemfinder.core.common.shared.SortType
import com.example.problemfinder.core.model.browse.BrowseFailure
import com.example.problemfinder.core.model.browse.BrowseOperationException
import com.example.problemfinder.core.model.browse.BrowsePage
import com.example.problemfinder.core.model.browse.BrowseSnapshot
import com.example.problemfinder.data.authentication.api.AuthSession
import com.example.problemfinder.data.authentication.api.AuthSessionProvider
import com.example.problemfinder.data.browse.api.BrowseRepository
import com.example.problemfinder.data.browse.impl.local.BrowseLocalDataSource
import com.example.problemfinder.data.browse.impl.remote.BrowseRemoteDataSource
import com.example.problemfinder.data.browse.impl.util.toModel
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

class BrowseRepositoryImpl @Inject constructor(
    private val sessions: AuthSessionProvider,
    private val remote: BrowseRemoteDataSource,
    private val local: BrowseLocalDataSource,
) : BrowseRepository {

    private val syncMutex = Mutex()

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeBrowse(
        category: Category,
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
        category: Category,
        sortType: SortType,
    ): Unit = browseCall {
        val session = requireSession()

        syncMutex.withLock {
            ensureSameAccount(session.userId)
            val page = fetchPage(session, category, sortType, cursor = null)
            ensureSameAccount(session.userId)
            local.replaceFirstPage(session.userId, category, sortType, page)
        }
    }

    override suspend fun loadNextPage(
        category: Category,
        sortType: SortType,
    ): Unit = browseCall {
        val session = requireSession()
        val query = local.getQuery(session.userId, category, sortType)
            ?: return@browseCall
        if (!query.hasSynced) return@browseCall
        val expectedCursor = query.nextCursor ?: return@browseCall

        syncMutex.withLock {
            ensureSameAccount(session.userId)
            val current = local.getQuery(session.userId, category, sortType)

            if (current?.hasSynced != true ||
                current.nextCursor != expectedCursor
            ) {
                return@withLock
            }

            val page = fetchPage(session, category, sortType, expectedCursor)

            if (page.nextCursor == expectedCursor) {
                throw BrowseOperationException(
                    BrowseFailure.INVALID_RESPONSE,
                    IllegalStateException("Browse cursor did not advance"),
                )
            }

            ensureSameAccount(session.userId)
            local.appendPage(
                userId = session.userId,
                category = category,
                sortType = sortType,
                expectedCursor = expectedCursor,
                page = page,
            )
        }
    }

    private suspend fun requireSession(): AuthSession =
        sessions.currentSession() ?: throw BrowseOperationException(
            BrowseFailure.SESSION_UNAVAILABLE,
            IllegalStateException("No usable Browse session"),
        )

    private suspend fun ensureSameAccount(userId: String) {
        if (sessions.observeUserId().first() != userId) {
            throw CancellationException("Browse account changed")
        }
    }

    private suspend fun fetchPage(
        session: AuthSession,
        category: Category,
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
            throw BrowseOperationException(BrowseFailure.INVALID_RESPONSE, error)
        }
    }

    private suspend fun <T> browseCall(block: suspend () -> T): T =
        try {
            block()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: BrowseOperationException) {
            throw error
        } catch (error: Exception) {
            val failure = when (error) {
                is ResponseException -> when (error.response.status.value) {
                    401 -> BrowseFailure.SESSION_UNAVAILABLE
                    in 400..499 -> BrowseFailure.REQUEST_REJECTED
                    in 500..599 -> BrowseFailure.SERVER
                    else -> BrowseFailure.UNKNOWN
                }

                is ContentConvertException,
                is SerializationException -> BrowseFailure.INVALID_RESPONSE

                is HttpRequestTimeoutException,
                is IOException -> BrowseFailure.NETWORK

                else -> BrowseFailure.UNKNOWN
            }

            throw BrowseOperationException(failure, error)
        }
}
