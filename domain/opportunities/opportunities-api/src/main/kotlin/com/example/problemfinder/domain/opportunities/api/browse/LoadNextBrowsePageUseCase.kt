package com.example.problemfinder.domain.opportunities.api.browse

import com.example.problemfinder.core.common.UseCase
import com.example.problemfinder.core.common.shared.OpportunityCategoryFilter
import com.example.problemfinder.core.common.shared.SortType


abstract class LoadNextBrowsePageUseCase : UseCase<LoadNextBrowsePageUseCase.Params, Unit>() {
    data class Params(val category: OpportunityCategoryFilter, val sort: SortType)
}
