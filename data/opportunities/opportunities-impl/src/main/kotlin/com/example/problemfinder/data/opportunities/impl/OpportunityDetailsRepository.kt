package com.example.problemfinder.data.opportunities.impl

import com.example.problemfinder.core.model.details.OpportunityDetailsPage
import com.example.problemfinder.core.model.details.OpportunityDetailsSnapshot
import com.example.problemfinder.core.model.opportunity.OpportunityFailure
import com.example.problemfinder.core.model.opportunity.OpportunityOperationException
import com.example.problemfinder.data.authentication.api.AuthSessionProvider
import com.example.problemfinder.data.opportunities.api.OpportunityDetailsRepository
import com.example.problemfinder.data.opportunities.impl.local.OpportunityDetailsLocalDataSource
import com.example.problemfinder.data.opportunities.impl.remote.OpportunityDetailsRemoteDataSource
import com.example.problemfinder.data.opportunities.impl.util.OpportunitySyncCoordinator
import com.example.problemfinder.data.opportunities.impl.util.ensureSameAccount
import com.example.problemfinder.data.opportunities.impl.util.opportunityCall
import com.example.problemfinder.data.opportunities.impl.util.requireSession
import com.example.problemfinder.data.opportunities.impl.util.toDetailsPage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

class OpportunityDetailsRepositoryImpl @Inject constructor(
    private val sessions: AuthSessionProvider,
    private val remote: OpportunityDetailsRemoteDataSource,
    private val local: OpportunityDetailsLocalDataSource,
    private val sync: OpportunitySyncCoordinator,
) : OpportunityDetailsRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeDetails(clusterId: Int): Flow<OpportunityDetailsSnapshot> =
        sessions.observeUserId()
            .distinctUntilChanged()
            .flatMapLatest { userId ->
                val emptySnapshot = OpportunityDetailsSnapshot(
                    userId = userId,
                    cacheReady = false
                )

                if (userId == null) {
                    flowOf(emptySnapshot.copy(cacheReady = true))
                } else {
                    local.observeDetails(userId, clusterId).onStart {
                        emit(emptySnapshot)
                    }
                }
            }

    override suspend fun refreshDetails(clusterId: Int): Unit = opportunityCall {
            require(clusterId > 0) { "clusterId must be nonnegative: $clusterId" }
            val session = requireSession(sessions)
            sync.mutex.withLock {

                var detailsPage: OpportunityDetailsPage

                ensureSameAccount(
                    userId = session.userId,
                    sessions = sessions
                )

                val details = remote.fetchDetails(
                    clusterId = clusterId,
                    accessToken = session.accessToken
                )

                try {
                    require(details.id == clusterId) {
                        "Error collecting cluster: requesting cluster $clusterId - response cluster ${details.id}"
                    }

                    require(details.page == 1) {
                        "Error collecting cluster: invalid page ${details.page} should be 1"
                    }

                    require(details.pageSize == 20) {
                        "Error collecting cluster: invalid pageSize ${details.pageSize} should be 20"
                    }

                    detailsPage = details.toDetailsPage()

                }catch (e: IllegalArgumentException) {
                    throw OpportunityOperationException(OpportunityFailure.INVALID_RESPONSE, e)

                }

                ensureSameAccount(
                    userId = session.userId,
                    sessions = sessions
                )

                local.storePage(
                    userId = session.userId,
                    page = detailsPage,
                    replace = true
                )
            }

    }

    override suspend fun loadNextEvidencePage(clusterId: Int) = opportunityCall {
        require(clusterId > 0) { "clusterId must be nonnegative: $clusterId" }
        val session = requireSession(sessions)
        sync.mutex.withLock {

            val cache = local.getOpportunityDetails(
                userId = session.userId,
                clusterId = clusterId
            )
            var detailsPage: OpportunityDetailsPage
            ensureSameAccount(
                userId = session.userId,
                sessions = sessions
            )

            val pageSize = cache.pageSize
            val nextPage = cache.lastLoadedPage + 1

            if(cache.hasSynced.not()) return@withLock
            if(cache.lastLoadedPage * cache.pageSize >= cache.totalPosts) return@withLock

            val response = remote.fetchDetails(
                clusterId = clusterId,
                accessToken = session.accessToken,
                page = nextPage,
                pageSize = pageSize
            )

            try {
                require(response.id == clusterId) {
                    "Error collecting cluster: requesting cluster $clusterId - response cluster ${response.id}"
                }

                require(response.page == nextPage) {
                    "Error collecting cluster: invalid page ${response.page} should be $nextPage"
                }

                require(response.pageSize == pageSize) {
                    "Error collecting cluster: invalid pageSize ${response.pageSize} should be $pageSize"
                }

                detailsPage = response.toDetailsPage()

            }catch (e: IllegalArgumentException) {
                throw OpportunityOperationException(OpportunityFailure.INVALID_RESPONSE, e)

            }

            ensureSameAccount(
                userId = session.userId,
                sessions = sessions
            )

            local.storePage(
                userId = session.userId,
                page = detailsPage,
                replace = false
            )
        }
    }
}
