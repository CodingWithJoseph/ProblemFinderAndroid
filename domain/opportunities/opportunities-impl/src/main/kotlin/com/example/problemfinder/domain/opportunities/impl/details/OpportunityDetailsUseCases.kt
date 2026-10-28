package com.example.problemfinder.domain.opportunities.impl.details

import com.example.problemfinder.core.model.details.OpportunityDetailsSnapshot
import com.example.problemfinder.domain.opportunities.api.details.ObserveOpportunityDetailsUseCase
import com.example.problemfinder.domain.opportunities.api.details.RefreshOpportunityDetailsUseCase
import com.example.problemfinder.domain.opportunities.api.details.LoadNextOpportunityEvidenceUseCase
import com.example.problemfinder.data.opportunities.api.OpportunityDetailsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Supplied delegation for step 4; the repository contains the unfinished feature work. */
class ObserveOpportunityDetailsUseCaseImpl @Inject constructor(
    private val repository: OpportunityDetailsRepository,
) : ObserveOpportunityDetailsUseCase() {
    override suspend fun execute(params: Params): Flow<OpportunityDetailsSnapshot> = repository.observeDetails(
        params.clusterId
    )
}

class RefreshOpportunityDetailsUseCaseImpl @Inject constructor(
    private val repository: OpportunityDetailsRepository,
) : RefreshOpportunityDetailsUseCase() {
    override suspend fun execute(params: Params): Unit = repository.refreshDetails(
        params.clusterId
    )
}

class LoadNextOpportunityEvidenceUseCaseImpl @Inject constructor(
    private val repository: OpportunityDetailsRepository,
) : LoadNextOpportunityEvidenceUseCase() {
    override suspend fun execute(params: Params): Unit = repository.loadNextEvidencePage(
        params.clusterId
    )
}
