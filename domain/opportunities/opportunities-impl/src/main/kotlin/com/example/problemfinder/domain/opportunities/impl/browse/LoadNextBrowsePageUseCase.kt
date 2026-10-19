package com.example.problemfinder.domain.opportunities.impl.browse

import com.example.problemfinder.data.opportunities.api.BrowseRepository
import com.example.problemfinder.domain.opportunities.api.browse.LoadNextBrowsePageUseCase
import javax.inject.Inject

class LoadNextBrowsePageUseCaseImpl @Inject constructor(
    private val repository: BrowseRepository,
) : LoadNextBrowsePageUseCase() {
    override suspend fun execute(params: Params) =
        repository.loadNextPage(params.category, params.sort)
}
