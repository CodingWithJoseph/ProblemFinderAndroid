package com.example.problemfinder.domain.browse.api

import com.example.problemfinder.core.common.UseCase
import com.example.problemfinder.core.common.shared.Category
import com.example.problemfinder.core.common.shared.SortType

abstract class RefreshBrowseUseCase : UseCase<RefreshBrowseUseCase.Params, Unit>() {
    data class Params(val category: Category, val sort: SortType)
}
