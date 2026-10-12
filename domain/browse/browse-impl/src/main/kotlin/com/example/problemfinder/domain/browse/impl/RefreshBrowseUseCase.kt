package com.example.problemfinder.domain.browse.impl

import com.example.problemfinder.data.browse.api.BrowseRepository
import com.example.problemfinder.domain.browse.api.RefreshBrowseUseCase
import javax.inject.Inject

class RefreshBrowseUseCaseImpl @Inject constructor(
    private val repository: BrowseRepository,
) : RefreshBrowseUseCase() {
    override suspend fun execute(params: Params) =
        repository.refresh(params.category, params.sort)
}
