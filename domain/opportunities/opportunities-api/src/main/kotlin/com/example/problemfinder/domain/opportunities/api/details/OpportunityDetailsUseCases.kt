package com.example.problemfinder.domain.opportunities.api.details

import com.example.problemfinder.core.common.ObservableUseCase
import com.example.problemfinder.core.common.UseCase
import com.example.problemfinder.core.model.details.OpportunityDetailsSnapshot

/**
 * DETAILS-4: These boundaries and their repository delegation are already supplied.
 * Int means cluster identity, not a navigation route or page number. Observe starts no HTTP call.
 * Add domain policy only when needed; HTTP/Room logic stays in the data implementation.
 */
abstract class ObserveOpportunityDetailsUseCase : ObservableUseCase<Int, OpportunityDetailsSnapshot>()
abstract class RefreshOpportunityDetailsUseCase : UseCase<Int, Unit>()
abstract class LoadNextOpportunityEvidenceUseCase : UseCase<Int, Unit>()
