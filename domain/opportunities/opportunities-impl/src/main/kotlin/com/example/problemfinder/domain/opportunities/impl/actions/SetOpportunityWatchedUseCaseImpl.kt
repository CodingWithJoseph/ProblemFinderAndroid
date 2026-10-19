package com.example.problemfinder.domain.opportunities.impl.actions

import com.example.problemfinder.data.opportunities.api.OpportunityActionsRepository
import com.example.problemfinder.domain.opportunities.api.actions.SetOpportunityWatchedUseCase
import javax.inject.Inject

class SetOpportunityWatchedUseCaseImpl @Inject constructor(
    private val repository: OpportunityActionsRepository,
) : SetOpportunityWatchedUseCase() {
    override suspend fun execute(params: Params) = repository.setWatched(params.userId, params.clusterId, params.watched)
}
