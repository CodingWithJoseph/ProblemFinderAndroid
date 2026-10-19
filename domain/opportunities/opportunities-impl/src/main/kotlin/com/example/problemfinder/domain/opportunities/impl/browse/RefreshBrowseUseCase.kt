package com.example.problemfinder.domain.opportunities.impl.browse

import com.example.problemfinder.data.opportunities.api.BrowseRepository
import com.example.problemfinder.domain.opportunities.api.browse.RefreshBrowseUseCase
import javax.inject.Inject

class RefreshBrowseUseCaseImpl @Inject constructor(
    private val repository: BrowseRepository,
) : RefreshBrowseUseCase() {
    override suspend fun execute(params: Params) =
        repository.refresh(params.category, params.sort)
}
