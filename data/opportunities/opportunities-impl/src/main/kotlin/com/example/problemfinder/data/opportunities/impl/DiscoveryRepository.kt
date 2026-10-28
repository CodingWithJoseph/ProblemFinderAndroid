package com.example.problemfinder.data.opportunities.impl

import com.example.problemfinder.core.model.opportunity.OpportunityFailure
import com.example.problemfinder.core.model.opportunity.OpportunityOperationException
import com.example.problemfinder.core.model.discovery.DiscoveryDashboard
import com.example.problemfinder.core.model.discovery.DiscoverySnapshot
import com.example.problemfinder.data.authentication.api.AuthSession
import com.example.problemfinder.data.authentication.api.AuthSessionProvider
import com.example.problemfinder.data.opportunities.api.DiscoveryRepository
import com.example.problemfinder.data.opportunities.impl.local.DiscoveryLocalDataSource
import com.example.problemfinder.data.opportunities.impl.remote.DiscoveryRemoteDataSource
import com.example.problemfinder.data.opportunities.impl.util.opportunityCall
import com.example.problemfinder.data.opportunities.impl.util.ensureSameAccount
import com.example.problemfinder.data.opportunities.impl.util.requireSession
import com.example.problemfinder.data.opportunities.impl.util.toModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import com.example.problemfinder.data.opportunities.impl.util.OpportunitySyncCoordinator
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject


class DiscoveryRepositoryImpl @Inject constructor(
    private val sessions: AuthSessionProvider,
    private val localDataSource: DiscoveryLocalDataSource,
    private val remoteDataSource: DiscoveryRemoteDataSource,
    private val sync: OpportunitySyncCoordinator,
) : DiscoveryRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeDiscovery(): Flow<DiscoverySnapshot> = sessions.observeUserId()
        .distinctUntilChanged()
        .flatMapLatest { userId ->
            val emptySnapshot = DiscoverySnapshot(
                userId = userId,
                dashboard = DiscoveryDashboard(
                    featuredCluster = null,
                    collections = listOf()
                ),
                cacheReady = false
            )

            if (userId == null) {
                flowOf(emptySnapshot.copy(cacheReady = true))
            } else {
                localDataSource.observeDashboard(userId).onStart {
                    emit(emptySnapshot)
                }
            }
        }

    override suspend fun refresh(): Unit = opportunityCall {
        val session = requireSession(sessions)

        sync.mutex.withLock {
            ensureSameAccount(session.userId, sessions)
            val dashboard = fetchDashboard(session)

            ensureSameAccount(session.userId, sessions)
            localDataSource.replaceDashboard(
                userId = session.userId,
                dashboard = dashboard
            )
        }
    }

    private suspend fun fetchDashboard(session: AuthSession): DiscoveryDashboard {
        val dto = remoteDataSource.fetchDashboard(
            accessToken = session.accessToken
        )

        return try {
            dto.toModel()
        } catch (error: IllegalArgumentException) {
            throw OpportunityOperationException(OpportunityFailure.INVALID_RESPONSE, error)
        }
    }
}
