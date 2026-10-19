package com.example.problemfinder.feature.opportunities.impl.browse

import androidx.lifecycle.SavedStateHandle
import com.example.problemfinder.core.common.shared.OpportunityCategoryFilter
import com.example.problemfinder.core.common.shared.SortType
import com.example.problemfinder.core.model.opportunity.OpportunityFailure
import com.example.problemfinder.core.model.opportunity.OpportunityOperationException
import com.example.problemfinder.core.model.browse.BrowseSnapshot
import com.example.problemfinder.domain.opportunities.api.actions.SetOpportunityWatchedUseCase
import com.example.problemfinder.core.mvi.BaseMVIViewModel
import com.example.problemfinder.core.mvi.RequestState
import com.example.problemfinder.domain.opportunities.api.browse.LoadNextBrowsePageUseCase
import com.example.problemfinder.domain.opportunities.api.browse.ObserveBrowseUseCase
import com.example.problemfinder.domain.opportunities.api.browse.RefreshBrowseUseCase
import com.example.problemfinder.feature.opportunities.api.OpportunitiesDestinations
import com.example.problemfinder.feature.opportunities.impl.util.OpportunityUiError
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
class BrowseViewModel @Inject constructor(
    private val observeBrowse: ObserveBrowseUseCase,
    private val refreshBrowse: RefreshBrowseUseCase,
    private val loadNextBrowsePage: LoadNextBrowsePageUseCase,
    savedStateHandle: SavedStateHandle,
    private val setWatched: SetOpportunityWatchedUseCase,
) : BaseMVIViewModel<BrowseIntent, BrowseState, BrowseEffect>(BrowseState()) {

    private val watchJobs = mutableMapOf<Int, Job>()
    private var watchVersion = 0L
    private var observationJob: Job? = null
    private var refreshJob: Job? = null
    private var pageJob: Job? = null
    private var queryVersion = 0L
    private var requestVersion = 0L
    private var needsInitialSync = false
    private var lastPageCursor: String? = null
    private var hasEntered = false

    init {
        val categoryName = savedStateHandle.get<String>(
            OpportunitiesDestinations.CATEGORY_ARGUMENT
        )

        val initialCategory = OpportunityCategoryFilter.entries.firstOrNull {
            it.name == categoryName
        } ?: OpportunityCategoryFilter.ALL

        startQuery(initialCategory, SortType.NEWEST)
    }

    override fun onIntent(intent: BrowseIntent) {
        when (intent) {
            is BrowseIntent.WatchCluster -> watchCluster(intent.clusterId)
            is BrowseIntent.OpenCluster -> sendEffect(BrowseEffect.NavigateToCluster(intent.clusterId))
            BrowseIntent.Entered -> {
                if (hasEntered) refreshCurrentQuery() else hasEntered = true
            }
            BrowseIntent.Reload -> refreshCurrentQuery()
            BrowseIntent.LoadNextPage -> loadNextPage(explicit = false)
            BrowseIntent.RetryNextPage -> loadNextPage(explicit = true)
            is BrowseIntent.ChangeSortType -> {
                if (intent.value != state.value.sort) {
                    startQuery(state.value.category, intent.value)
                }
            }
            is BrowseIntent.ChangeCategory -> {
                if (intent.value != state.value.category) {
                    startQuery(intent.value, state.value.sort)
                }
            }
        }
    }

    private fun startQuery(
        category: OpportunityCategoryFilter,
        sort: SortType,
        keepCache: Boolean = false,
    ) {
        queryVersion += 1
        val version = queryVersion
        invalidateRequests()
        observationJob?.cancel()
        needsInitialSync = true

        reduce {
            if (keepCache) {
                copy(
                    cacheReady = false,
                    observationRequest = RequestState.Loading,
                    refreshRequest = RequestState.Idle,
                    pageRequest = RequestState.Idle,
                )
            } else {
                BrowseState(category = category, sort = sort, userId = userId, pendingClusterIds = pendingClusterIds)
            }
        }

        observationJob = safeLaunch {
            try {
                observeBrowse(ObserveBrowseUseCase.Params(category, sort))
                    .collect { snapshot ->
                        if (version == queryVersion) acceptSnapshot(snapshot)
                    }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (version == queryVersion) onError(error)
            }
        }
    }

    private fun acceptSnapshot(snapshot: BrowseSnapshot) {
        if (state.value.userId != snapshot.userId) {
            cancelWatchActions()
            invalidateRequests()
            needsInitialSync = true
            reduce {
                BrowseState(
                    category = category,
                    sort = sort,
                    userId = snapshot.userId,
                )
            }
        }

        if (!snapshot.cacheReady) {
            reduce {
                copy(cacheReady = false, observationRequest = RequestState.Loading)
            }
            return
        }

        reduce {
            copy(
                userId = snapshot.userId,
                data = snapshot.clusters,
                cursor = snapshot.nextCursor,
                hasSynced = snapshot.hasSynced,
                cacheReady = true,
                observationRequest = RequestState.Success(Unit),
            )
        }

        if (snapshot.userId != null && needsInitialSync) {
            needsInitialSync = false
            refreshCurrentQuery()
        }
    }

    private fun refreshCurrentQuery() {
        val current = state.value
        if (current.observationRequest is RequestState.Error) {
            startQuery(current.category, current.sort, keepCache = true)
            return
        }
        if (!current.canRefresh) return

        val version = requestVersion
        val params = RefreshBrowseUseCase.Params(current.category, current.sort)
        lastPageCursor = null
        reduce {
            copy(refreshRequest = RequestState.Loading, pageRequest = RequestState.Idle)
        }

        refreshJob = safeLaunch {
            try {
                refreshBrowse(params)
                if (version == requestVersion) {
                    reduce { copy(refreshRequest = RequestState.Success(Unit)) }
                }
            } catch (cancelled: CancellationException) {
                if (version == requestVersion) {
                    reduce { copy(refreshRequest = RequestState.Idle) }
                }
                throw cancelled
            } catch (error: Exception) {
                if (version == requestVersion) {
                    reduce { copy(refreshRequest = RequestState.Error(error.toUiError())) }
                }
            }
        }
    }

    private fun loadNextPage(explicit: Boolean) {
        val current = state.value
        if (!current.canLoadNextPage) return
        if (!explicit && !current.canAutoLoadNextPage) return
        if (!explicit && current.cursor == lastPageCursor) return

        val version = requestVersion
        val params = LoadNextBrowsePageUseCase.Params(current.category, current.sort)
        lastPageCursor = current.cursor
        reduce { copy(pageRequest = RequestState.Loading) }

        pageJob = safeLaunch {
            try {
                loadNextBrowsePage(params)
                if (version == requestVersion) {
                    reduce { copy(pageRequest = RequestState.Success(Unit)) }
                }
            } catch (cancelled: CancellationException) {
                if (version == requestVersion) {
                    reduce { copy(pageRequest = RequestState.Idle) }
                }
                throw cancelled
            } catch (error: Exception) {
                if (version == requestVersion) {
                    reduce { copy(pageRequest = RequestState.Error(error.toUiError())) }
                }
            }
        }
    }

    private fun watchCluster(clusterId: Int) {
        val current = state.value
        val userId = current.userId ?: return
        if (!current.cacheReady || clusterId in current.pendingClusterIds) return
        val cluster = current.data.firstOrNull { it.id == clusterId } ?: return
        val version = watchVersion
        reduce { copy(pendingClusterIds = pendingClusterIds + clusterId) }
        val job = safeLaunch {
            try {
                setWatched(SetOpportunityWatchedUseCase.Params(userId, clusterId, !cluster.isWatched))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (version == watchVersion && state.value.userId == userId) {
                    val failure = (error as? OpportunityOperationException)?.failure ?: OpportunityFailure.UNKNOWN
                    sendEffect(BrowseEffect.ShowError(OpportunityUiError(failure)))
                }
            } finally {
                if (version == watchVersion && state.value.userId == userId) {
                    reduce { copy(pendingClusterIds = pendingClusterIds - clusterId) }
                }
            }
        }
        watchJobs[clusterId] = job
        job.invokeOnCompletion { if (watchJobs[clusterId] === job) watchJobs.remove(clusterId) }
    }

    private fun cancelWatchActions() {
        watchVersion += 1
        watchJobs.values.toList().forEach { it.cancel() }
        watchJobs.clear()
    }

    private fun invalidateRequests() {
        requestVersion += 1
        refreshJob?.cancel()
        pageJob?.cancel()
        refreshJob = null
        pageJob = null
        lastPageCursor = null
    }

    override fun onError(error: Throwable) {
        invalidateRequests()
        reduce {
            copy(
                observationRequest = RequestState.Error(error.toUiError()),
                refreshRequest = RequestState.Idle,
                pageRequest = RequestState.Idle,
            )
        }
    }

    private fun Throwable.toUiError() = OpportunityUiError(
        (this as? OpportunityOperationException)?.failure ?: OpportunityFailure.UNKNOWN,
    )
}
