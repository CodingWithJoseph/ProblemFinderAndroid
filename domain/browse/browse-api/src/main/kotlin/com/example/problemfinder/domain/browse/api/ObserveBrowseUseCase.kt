package com.example.problemfinder.domain.browse.api

import com.example.problemfinder.core.common.ObservableUseCase
import com.example.problemfinder.core.common.shared.Category
import com.example.problemfinder.core.common.shared.SortType
import com.example.problemfinder.core.model.browse.BrowseSnapshot

abstract class ObserveBrowseUseCase: ObservableUseCase<ObserveBrowseUseCase.Params, BrowseSnapshot>() {
    data class Params(val category: Category, val sort: SortType)
}
