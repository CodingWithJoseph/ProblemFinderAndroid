package com.example.problemfinder.domain.opportunities.api.search

import com.example.problemfinder.core.common.ObservableUseCase
import com.example.problemfinder.core.common.UseCase
import com.example.problemfinder.core.model.search.OpportunitySearchDraft
import com.example.problemfinder.core.model.search.OpportunitySearchQuery
import com.example.problemfinder.core.model.search.OpportunitySearchSnapshot

/**
 * SEARCH-4: Wiring is supplied. The ViewModel captures a submitted query and passes that immutable
 * value to every operation for the result set. Editable text must not change a request in flight.
 * TODO [SEARCH-1] Put shared input validation/normalization at this boundary or a domain helper
 * before delegating; keep network DTOs and cache keys inside the data implementation.
 */
abstract class ObserveOpportunitySearchUseCase : ObservableUseCase<OpportunitySearchQuery, OpportunitySearchSnapshot>()
abstract class RefreshOpportunitySearchUseCase : UseCase<OpportunitySearchQuery, Unit>()
abstract class LoadNextOpportunitySearchPageUseCase : UseCase<OpportunitySearchQuery, Unit>()

abstract class PrepareOpportunitySearchDraftUseCase : UseCase<PrepareOpportunitySearchDraftUseCase.Params, OpportunitySearchDraft>() {
    data class Params(val message: String, val currentDraft: OpportunitySearchQuery?)
}
