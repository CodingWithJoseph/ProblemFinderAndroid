package com.example.problemfinder.domain.opportunities.api.browse

import com.example.problemfinder.core.common.ObservableUseCase
import com.example.problemfinder.core.common.shared.OpportunityCategoryFilter
import com.example.problemfinder.core.common.shared.SortType
import com.example.problemfinder.core.model.browse.BrowseSnapshot

abstract class ObserveBrowseUseCase: ObservableUseCase<ObserveBrowseUseCase.Params, BrowseSnapshot>() {
    data class Params(val category: OpportunityCategoryFilter, val sort: SortType)
}
