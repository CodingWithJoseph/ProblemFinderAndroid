package com.example.problemfinder.domain.opportunities.impl.browse

import com.example.problemfinder.core.model.browse.BrowseSnapshot
import com.example.problemfinder.data.opportunities.api.BrowseRepository
import com.example.problemfinder.domain.opportunities.api.browse.ObserveBrowseUseCase
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveBrowseUseCaseImpl @Inject constructor(
    private val repository: BrowseRepository
) : ObserveBrowseUseCase() {
    override suspend fun execute(params: Params): Flow<BrowseSnapshot> = repository
        .observeBrowse(params.category, params.sort)
}
