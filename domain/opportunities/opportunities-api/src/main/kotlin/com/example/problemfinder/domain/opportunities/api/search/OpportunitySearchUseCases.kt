package com.example.problemfinder.domain.opportunities.api.search

import com.example.problemfinder.core.common.ObservableUseCase
import com.example.problemfinder.core.common.UseCase
import com.example.problemfinder.core.model.search.OpportunitySearchDraft
import com.example.problemfinder.core.model.search.OpportunitySearchQuery
import com.example.problemfinder.core.model.search.OpportunitySearchSnapshot

abstract class LoadOpportunitySearchOptionsUseCase : UseCase<Unit, com.example.problemfinder.core.model.search.OpportunitySearchOptions>()

abstract class ObserveOpportunitySearchUseCase : ObservableUseCase<OpportunitySearchQuery, OpportunitySearchSnapshot>()
abstract class RefreshOpportunitySearchUseCase : UseCase<OpportunitySearchQuery, Unit>()
abstract class LoadNextOpportunitySearchPageUseCase : UseCase<OpportunitySearchQuery, Unit>()

abstract class PrepareOpportunitySearchDraftUseCase : UseCase<PrepareOpportunitySearchDraftUseCase.Params, OpportunitySearchDraft>() {
    data class Params(val message: String, val currentDraft: OpportunitySearchQuery?)
}
