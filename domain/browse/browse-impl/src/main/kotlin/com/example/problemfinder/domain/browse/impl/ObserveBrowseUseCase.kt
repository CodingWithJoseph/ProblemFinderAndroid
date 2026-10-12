package com.example.problemfinder.domain.browse.impl

import com.example.problemfinder.core.model.browse.BrowseSnapshot
import com.example.problemfinder.data.browse.api.BrowseRepository
import com.example.problemfinder.domain.browse.api.ObserveBrowseUseCase
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveBrowseUseCaseImpl @Inject constructor(
    private val repository: BrowseRepository
) : ObserveBrowseUseCase() {
    override suspend fun execute(params: Params): Flow<BrowseSnapshot> = repository
        .observeBrowse(params.category, params.sort)
}
