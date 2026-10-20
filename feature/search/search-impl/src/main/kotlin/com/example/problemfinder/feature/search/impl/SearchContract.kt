package com.example.problemfinder.feature.search.impl

import com.example.problemfinder.core.model.opportunity.Opportunity
import com.example.problemfinder.core.model.opportunity.OpportunityFailure
import com.example.problemfinder.core.model.search.OpportunitySearchDraft
import com.example.problemfinder.core.model.search.OpportunitySearchQuery
import com.example.problemfinder.core.mvi.RequestState

/**
 * START HERE - complete Details first, then build manual Search before assistant interpretation.
 * The files already exist. Complete their bodies; no new competing repository/DTO is needed.
 *
 * SEARCH-1: trustworthy request/response values, without live HTTP or Room.
 * 1A. core/model/search/OpportunitySearch.kt: query normalization/validation and supported sorts.
 * 1B. .../model/dto/OpportunitySearchDto.kt: verify wire keys and resolve legacy missing category/text.
 * 1C. .../util/OpportunitySearchMapper.kt: query -> request DTO and response DTO -> validated page.
 *     Write mapper/model unit tests now; a successful fixture must retain backend relevance order.
 *
 * SEARCH-2: persist ONE account + normalized submitted query's results.
 * 2A. cacheKey() + OpportunitySearchEntities.kt: query metadata and ordered shared-card memberships.
 * 2B. OpportunitySearchDao.kt: joins/read models, write queries, registration and migration.
 * 2C. OpportunitySearchLocalDataSource.kt: entity <-> model assembly and atomic replace/append.
 *     Keep hasSynced, total and nextOffset in metadata; no rows alone is not successful zero results.
 *
 * SEARCH-3/4: real fetching and orchestration.
 * 3A. OpportunitySearchRemoteDataSource.fetchResults: POST structured /clusters/search/query.
 * 3B. OpportunitySearchRepositoryImpl: account-aware observation and offset-0 refresh into Room.
 * 3C. Same repository: read saved nextOffset, append, and advance progress only after commit.
 * 4. Domain use cases/Hilt bindings already delegate to the repository. Read the call path; the
 *    existing wiring does not mean the unfinished repository body is already implemented.
 *
 * SEARCH-5/6/7: connect and finish the screen.
 * 5. SearchViewModel.kt: immutable submitted query, observation, request generations, watch/error jobs.
 * 6. SearchScreen.kt + Navigation.kt: filters, validation, initial/empty/content/error/paging states,
 *    shared Details navigation and Back restoration. Keep typed draft input separate from shown results.
 * 7. implementation-guides/SearchViewModelTest.kt.template lists the cases to implement throughout.
 *
 * SEARCH-8: optional assistant stage, only after manual Search works.
 * Prepare a draft, show clarification/confirmation, then execute through the SAME submit path when
 * the user confirms. Interpretation alone never replaces results or marks their cache as synced.
 *
 * Runtime: input draft -> Submit captures query -> observe query/account cache -> state -> screen.
 * Refresh: HTTP DTO -> validated page -> entities -> transaction -> observer. Page 2 uses the server
 * nextOffset, not Browse's cursor or UI item count. Stop at each file's finish check before proceeding.
 */
data class SearchState(
    val draftQuery: OpportunitySearchQuery = OpportunitySearchQuery(),
    val submittedQuery: OpportunitySearchQuery? = null,
    val interpretation: OpportunitySearchDraft? = null,
    val userId: String? = null,
    val items: List<Opportunity> = emptyList(),
    val total: Int = 0,
    val canLoadMore: Boolean = false,
    val cacheReady: Boolean = false,
    val hasSynced: Boolean = false,
    val observationRequest: RequestState<Unit, OpportunityFailure> = RequestState.Idle,
    val refreshRequest: RequestState<Unit, OpportunityFailure> = RequestState.Idle,
    val pageRequest: RequestState<Unit, OpportunityFailure> = RequestState.Idle,
    val interpretationRequest: RequestState<Unit, OpportunityFailure> = RequestState.Idle,
    val pendingClusterIds: Set<Int> = emptySet(),
    val implementationPending: Boolean = true,
)

sealed interface SearchIntent {
    data object Entered : SearchIntent
    data class QueryChanged(val value: String) : SearchIntent
    data class FiltersChanged(val value: OpportunitySearchQuery) : SearchIntent
    data object Submit : SearchIntent
    data object Reload : SearchIntent
    data object LoadNextPage : SearchIntent
    data object RetryNextPage : SearchIntent
    data object InterpretQuery : SearchIntent
    data object ConfirmDraft : SearchIntent
    data class OpenCluster(val clusterId: Int) : SearchIntent
    data class WatchCluster(val clusterId: Int) : SearchIntent
}

sealed interface SearchEffect {
    data class NavigateToCluster(val clusterId: Int) : SearchEffect
    data class ActionFailed(val failure: OpportunityFailure) : SearchEffect
}
