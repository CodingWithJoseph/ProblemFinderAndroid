package com.example.problemfinder.data.opportunities.impl

import com.example.problemfinder.core.model.opportunity.OpportunityFailure
import com.example.problemfinder.core.model.opportunity.OpportunityOperationException
import com.example.problemfinder.data.authentication.api.AuthSessionProvider
import com.example.problemfinder.data.opportunities.api.OpportunityActionsRepository
import com.example.problemfinder.data.opportunities.impl.db.OpportunityDao
import com.example.problemfinder.data.opportunities.impl.remote.OpportunityActionsRemoteDataSource
import com.example.problemfinder.data.opportunities.impl.util.OpportunitySyncCoordinator
import com.example.problemfinder.data.opportunities.impl.util.ensureSameAccount
import com.example.problemfinder.data.opportunities.impl.util.opportunityCall
import com.example.problemfinder.data.opportunities.impl.util.requireSession
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.withLock

class OpportunityActionsRepositoryImpl @Inject constructor(
    private val sessions: AuthSessionProvider,
    private val remote: OpportunityActionsRemoteDataSource,
    private val dao: OpportunityDao,
    private val sync: OpportunitySyncCoordinator,
) : OpportunityActionsRepository {
    override suspend fun setWatched(userId: String, clusterId: Int, watched: Boolean): Unit = opportunityCall {
        require(clusterId > 0)
        val session = requireSession(sessions)
        if (session.userId != userId) throw CancellationException("Opportunity account changed")
        sync.mutex.withLock {
            ensureSameAccount(userId, sessions)
            val response = remote.setWatched(clusterId, watched, session.accessToken)
            if (response.isWatched != watched) throw OpportunityOperationException(
                OpportunityFailure.INVALID_RESPONSE,
                IllegalStateException("Unexpected opportunity membership"),
            )
            ensureSameAccount(userId, sessions)
            // Both screens join this same account-scoped row. Room invalidates
            // all affected queries, including every occurrence on the dashboard.
            dao.setWatched(userId, clusterId, response.isWatched)
        }
    }
}
