package com.example.problemfinder.data.browse.impl.remote

import com.example.problemfinder.core.common.shared.Category
import com.example.problemfinder.core.common.shared.SortType
import com.example.problemfinder.data.browse.impl.model.BrowseApiConfig
import com.example.problemfinder.data.browse.impl.model.dto.BrowsePageDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import javax.inject.Inject

class BrowseRemoteDataSource @Inject constructor(
    private val client: HttpClient,
    private val config: BrowseApiConfig
) {

    suspend fun fetchBrowsePage(
        category: Category,
        sortType: SortType,
        accessToken: String,
        cursor: String?,
        limit: Int = 20
    ): BrowsePageDto = client.get("${config.baseUrl.trimEnd('/')}/clusters/browse/page") {
        bearerAuth(accessToken)
        parameter("category",  category.apiValue)
        parameter("sort", sortType.apiValue)
        parameter("limit", limit)

        cursor?.let {
            parameter("cursor", it)
        }
    }.body()
}

private val Category.apiValue: String
    get() = when (this) {
        Category.ALL -> "all"
    }

private val SortType.apiValue: String
    get() = when (this) {
        SortType.NEWEST -> "newest"
        SortType.OLDEST -> "oldest"
        SortType.LARGEST -> "largest"
        SortType.SMALLEST -> "smallest"
    }
