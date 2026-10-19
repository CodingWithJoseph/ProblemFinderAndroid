package com.example.problemfinder.domain.opportunities.api.browse

import com.example.problemfinder.core.common.UseCase
import com.example.problemfinder.core.common.shared.OpportunityCategoryFilter
import com.example.problemfinder.core.common.shared.SortType

/** Replaces all cached pages for the query with a fresh first page; observers receive the result. */
abstract class RefreshBrowseUseCase : UseCase<RefreshBrowseUseCase.Params, Unit>() {
    data class Params(val category: OpportunityCategoryFilter, val sort: SortType)
}
