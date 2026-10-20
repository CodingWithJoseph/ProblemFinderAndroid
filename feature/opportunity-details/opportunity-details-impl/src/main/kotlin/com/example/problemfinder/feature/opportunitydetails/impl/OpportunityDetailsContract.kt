package com.example.problemfinder.feature.opportunitydetails.impl

import com.example.problemfinder.core.model.details.OpportunityDetails
import com.example.problemfinder.core.model.details.OpportunityEvidence
import com.example.problemfinder.core.model.opportunity.Opportunity
import com.example.problemfinder.core.model.opportunity.OpportunityFailure
import com.example.problemfinder.core.mvi.RequestState

/**
 * START HERE - this scaffold already supplies the files and wiring. Complete their TODO bodies.
 * Work through Details before Search so both Browse and Search can open one finished destination.
 *
 * First milestone: validated app data, with NO database or live request yet (DETAILS-1).
 * 1A. core/model/details/OpportunityDetails.kt: finish constructor checks on the existing models.
 * 1B. data/opportunities/.../model/dto/OpportunityDetailsDto.kt: review the existing wire fields.
 * 1C. .../util/OpportunityDetailsMapper.kt: write DTO -> OpportunityDetailsPage, including nested
 *     evidence/date mapping. Add mapper/model unit tests using fixtures, then stop and check them.
 *
 * Second milestone: persisted data you can reopen offline (DETAILS-2).
 * 2A. .../model/entity/OpportunityDetailsEntities.kt: finish schema and model <-> entity mappings.
 * 2B. .../db/OpportunityDetailsDao.kt: write ordered account-scoped read/write queries and register
 *     the DAO/entities on OpportunitiesDatabase. Add/test the migration and its Hilt DAO binding.
 * 2C. .../local/OpportunityDetailsLocalDataSource.kt: assemble cached snapshots and atomic writes.
 *
 * Third milestone: fetch -> save -> observe (DETAILS-3/4).
 * 3A. .../remote/OpportunityDetailsRemoteDataSource.kt: authenticated GET, returning only a DTO.
 * 3B. .../OpportunityDetailsRepositoryImpl.kt: account observation and page-1 refresh orchestration.
 * 3C. Same repository: append evidence pages using saved progress. Test failure/retry/account switch.
 * 4. Domain use cases and Hilt bindings ALREADY delegate to that repository. Read them to trace
 *    the call, but no replacement implementation is needed just to connect the feature.
 *
 * Fourth milestone: usable screen (DETAILS-5/6/7).
 * 5. OpportunityDetailsViewModel.kt: consume snapshots; coordinate refresh/page/watch jobs and errors.
 * 6. OpportunityDetailsScreen.kt + Navigation.kt: finish content/evidence/states and external links.
 * 7. implementation-guides/OpportunityDetailsViewModelTest.kt.template lists the behavior tests.
 *    Write tests during each milestone, not only after the full screen is finished.
 *
 * Runtime paths to keep separate:
 * Opening: route ID -> Entered -> observe use case -> repository -> Room -> snapshot -> state -> UI.
 * Refresh: Reload -> refresh use case -> HTTP DTO -> validated model -> entities -> Room transaction.
 * The Room update then travels through the FIRST path. Refresh does not return screen content.
 *
 * Enable/remove implementationPending only when the corresponding real operation is connected.
 * Passing builds alone do not complete a milestone: use each file's finish check before moving on.
 */
data class OpportunityDetailsState(
    val clusterId: Int? = null,
    val userId: String? = null,
    val details: OpportunityDetails? = null,
    val cachedOpportunity: Opportunity? = null,
    val topEvidence: List<OpportunityEvidence> = emptyList(),
    val posts: List<OpportunityEvidence> = emptyList(),
    val canLoadMore: Boolean = false,
    val cacheReady: Boolean = false,
    val hasSynced: Boolean = false,
    val observationRequest: RequestState<Unit, OpportunityFailure> = RequestState.Idle,
    val refreshRequest: RequestState<Unit, OpportunityFailure> = RequestState.Idle,
    val pageRequest: RequestState<Unit, OpportunityFailure> = RequestState.Idle,
    val isWatchPending: Boolean = false,
    val implementationPending: Boolean = true,
)

sealed interface OpportunityDetailsIntent {
    data class Entered(val clusterId: Int) : OpportunityDetailsIntent
    data object Reload : OpportunityDetailsIntent
    data object LoadNextEvidencePage : OpportunityDetailsIntent
    data object RetryEvidencePage : OpportunityDetailsIntent
    data object WatchClicked : OpportunityDetailsIntent
    data class OpenEvidence(val evidenceId: Int) : OpportunityDetailsIntent
    data object BackClicked : OpportunityDetailsIntent
}

sealed interface OpportunityDetailsEffect {
    data object NavigateBack : OpportunityDetailsEffect
    data class OpenEvidenceLink(val url: String) : OpportunityDetailsEffect
    data class ActionFailed(val failure: OpportunityFailure) : OpportunityDetailsEffect
}
