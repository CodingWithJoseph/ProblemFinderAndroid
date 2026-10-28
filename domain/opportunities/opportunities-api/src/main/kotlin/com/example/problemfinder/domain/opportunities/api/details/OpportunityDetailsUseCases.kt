package com.example.problemfinder.domain.opportunities.api.details

import com.example.problemfinder.core.common.ObservableUseCase
import com.example.problemfinder.core.common.UseCase
import com.example.problemfinder.core.model.details.OpportunityDetailsSnapshot


abstract class ObserveOpportunityDetailsUseCase :
    ObservableUseCase<ObserveOpportunityDetailsUseCase.Params, OpportunityDetailsSnapshot>() {
    data class Params(
        val clusterId: Int
    )
}
abstract class RefreshOpportunityDetailsUseCase : UseCase<RefreshOpportunityDetailsUseCase.Params, Unit>() {
    data class Params(
        val clusterId: Int
    )
}

abstract class LoadNextOpportunityEvidenceUseCase : UseCase<LoadNextOpportunityEvidenceUseCase.Params, Unit>() {
    data class Params(
        val clusterId: Int
    )
}
