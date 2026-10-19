package com.example.problemfinder.data.opportunities.api

import com.example.problemfinder.core.common.shared.OpportunityCategoryFilter
import com.example.problemfinder.core.common.shared.SortType
import com.example.problemfinder.core.model.browse.BrowsePage
import com.example.problemfinder.core.model.browse.BrowseSnapshot
import kotlinx.coroutines.flow.Flow

interface BrowseRepository {

    fun observeBrowse(category: OpportunityCategoryFilter, sortType: SortType): Flow<BrowseSnapshot>
    suspend fun refresh(category: OpportunityCategoryFilter, sortType: SortType)
    suspend fun loadNextPage(category: OpportunityCategoryFilter, sortType: SortType
    )
}
