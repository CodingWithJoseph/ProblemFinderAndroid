package com.example.problemfinder.data.browse.api

import com.example.problemfinder.core.common.shared.Category
import com.example.problemfinder.core.common.shared.SortType
import com.example.problemfinder.core.model.browse.BrowsePage
import com.example.problemfinder.core.model.browse.BrowseSnapshot
import kotlinx.coroutines.flow.Flow

interface BrowseRepository {

    fun observeBrowse(category: Category, sortType: SortType): Flow<BrowseSnapshot>
    suspend fun refresh(category: Category, sortType: SortType)
    suspend fun loadNextPage(category: Category, sortType: SortType
    )
}
