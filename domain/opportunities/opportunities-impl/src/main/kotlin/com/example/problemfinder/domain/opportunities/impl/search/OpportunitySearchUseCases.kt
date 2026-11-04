package com.example.problemfinder.domain.opportunities.impl.search

import com.example.problemfinder.core.model.search.OpportunitySearchQuery
import com.example.problemfinder.core.model.search.OpportunitySearchSnapshot
import com.example.problemfinder.core.model.search.OpportunitySearchDraft
import com.example.problemfinder.domain.opportunities.api.search.ObserveOpportunitySearchUseCase
import com.example.problemfinder.domain.opportunities.api.search.RefreshOpportunitySearchUseCase
import com.example.problemfinder.domain.opportunities.api.search.LoadNextOpportunitySearchPageUseCase
import com.example.problemfinder.domain.opportunities.api.search.PrepareOpportunitySearchDraftUseCase
import com.example.problemfinder.data.opportunities.api.OpportunitySearchRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveOpportunitySearchUseCaseImpl @Inject constructor(
    private val repository: OpportunitySearchRepository,
) : ObserveOpportunitySearchUseCase() {
    override suspend fun execute(params: OpportunitySearchQuery): Flow<OpportunitySearchSnapshot> = repository.observeSearch(params)
}

class RefreshOpportunitySearchUseCaseImpl @Inject constructor(
    private val repository: OpportunitySearchRepository,
) : RefreshOpportunitySearchUseCase() {
    override suspend fun execute(params: OpportunitySearchQuery): Unit = repository.refreshSearch(params)
}

class LoadNextOpportunitySearchPageUseCaseImpl @Inject constructor(
    private val repository: OpportunitySearchRepository,
) : LoadNextOpportunitySearchPageUseCase() {
    override suspend fun execute(params: OpportunitySearchQuery): Unit = repository.loadNextSearchPage(params)
}

class LoadOpportunitySearchOptionsUseCaseImpl @Inject constructor(
    private val repository: OpportunitySearchRepository,
) : com.example.problemfinder.domain.opportunities.api.search.LoadOpportunitySearchOptionsUseCase() {
    override suspend fun execute(params: Unit) = repository.filterOptions()
}

class PrepareOpportunitySearchDraftUseCaseImpl @Inject constructor(
    private val repository: OpportunitySearchRepository,
) : PrepareOpportunitySearchDraftUseCase() {
    override suspend fun execute(params: PrepareOpportunitySearchDraftUseCase.Params): OpportunitySearchDraft = repository.prepareDraft(params.message, params.currentDraft)
}
