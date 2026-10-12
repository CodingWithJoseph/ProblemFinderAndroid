package com.example.problemfinder.domain.browse.impl

import com.example.problemfinder.data.browse.api.BrowseRepository
import com.example.problemfinder.domain.browse.api.LoadNextBrowsePageUseCase
import javax.inject.Inject

class LoadNextBrowsePageUseCaseImpl @Inject constructor(
    private val repository: BrowseRepository,
) : LoadNextBrowsePageUseCase() {
    override suspend fun execute(params: Params) =
        repository.loadNextPage(params.category, params.sort)
}
