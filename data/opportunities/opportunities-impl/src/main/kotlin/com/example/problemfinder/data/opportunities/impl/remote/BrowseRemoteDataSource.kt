package com.example.problemfinder.data.opportunities.impl.remote

import com.example.problemfinder.core.common.shared.OpportunityCategoryFilter
import com.example.problemfinder.core.common.shared.SortType
import com.example.problemfinder.data.opportunities.impl.model.OpportunitiesApiConfig
import com.example.problemfinder.data.opportunities.impl.model.dto.BrowsePageDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import javax.inject.Inject

class BrowseRemoteDataSource @Inject constructor(
    private val client: HttpClient,
    private val config: OpportunitiesApiConfig
) {
    suspend fun fetchBrowsePage(
        category: OpportunityCategoryFilter,
        sortType: SortType,
        accessToken: String,
        cursor: String?,
        limit: Int = 20
    ): BrowsePageDto = client.get("${config.baseUrl.trimEnd('/')}/clusters/browse/page") {
        bearerAuth(accessToken)
        parameter("category", category.apiValue)
        parameter("sort", sortType.apiValue)
        parameter("limit", limit)

        cursor?.let {
            parameter("cursor", it)
        }
    }.body()
}

private val OpportunityCategoryFilter.apiValue: String
    get() = when (this) {
        OpportunityCategoryFilter.ALL -> "all"
        OpportunityCategoryFilter.BUILD_AND_TOOLING -> "build_and_tooling"
        OpportunityCategoryFilter.DATA_AND_APIS -> "data_and_apis"
        OpportunityCategoryFilter.WORKFLOW_AND_AUTOMATION -> "workflow_and_automation"
        OpportunityCategoryFilter.UNCATEGORIZED -> "uncategorized"
    }

private val SortType.apiValue: String
    get() = when (this) {
        SortType.NEWEST -> "newest"
        SortType.OLDEST -> "oldest"
        SortType.LARGEST -> "largest"
        SortType.SMALLEST -> "smallest"
    }
