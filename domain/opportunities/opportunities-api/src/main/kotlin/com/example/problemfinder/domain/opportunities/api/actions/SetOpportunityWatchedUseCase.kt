package com.example.problemfinder.domain.opportunities.api.actions

import com.example.problemfinder.core.common.UseCase

/** Sets a cluster's watch state for the supplied account; the repository rejects a different active account. */
abstract class SetOpportunityWatchedUseCase : UseCase<SetOpportunityWatchedUseCase.Params, Unit>() {
    data class Params(val userId: String, val clusterId: Int, val watched: Boolean)
}
