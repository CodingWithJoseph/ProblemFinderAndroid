package com.example.problemfinder.feature.search.impl

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import com.example.problemfinder.core.model.opportunity.OpportunityFailure
import com.example.problemfinder.core.model.opportunity.OpportunityOperationException
import com.example.problemfinder.core.model.search.OpportunitySearchQuery
import com.example.problemfinder.core.model.search.OpportunitySearchSort
import com.example.problemfinder.core.mvi.BaseMVIViewModel
import com.example.problemfinder.core.mvi.RequestState
import com.example.problemfinder.domain.opportunities.api.actions.SetOpportunityWatchedUseCase
import com.example.problemfinder.domain.opportunities.api.search.*
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlin.time.Instant
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val observeSearch: ObserveOpportunitySearchUseCase,
    private val refreshSearch: RefreshOpportunitySearchUseCase,
    private val loadNextPage: LoadNextOpportunitySearchPageUseCase,
    private val prepareDraft: PrepareOpportunitySearchDraftUseCase,
    private val loadOptions: LoadOpportunitySearchOptionsUseCase,
    private val setWatched: SetOpportunityWatchedUseCase,
) : BaseMVIViewModel<SearchIntent, SearchState, SearchEffect>(SearchState()) {
    private var entered = false
    private var observation: Job? = null
    private var request: Job? = null
    private var interpretation: Job? = null
    private var options: Job? = null
    private val watches = mutableMapOf<Int, Job>()
    private var generation = 0L
    private var assistantVersion = 0L

    override fun onIntent(intent: SearchIntent) {
        when (intent) {
            SearchIntent.Entered -> enter()
            SearchIntent.OpenFilters -> openFilters()
            SearchIntent.RetryOptions -> fetchOptions()
            SearchIntent.OpenAssistant -> sendEffect(SearchEffect.NavigateToAssistant)
            SearchIntent.ClosePanel -> sendEffect(SearchEffect.NavigateBack)
            is SearchIntent.SourceToggled -> editFilters { copy(sources = sources.toggle(intent.value)) }
            is SearchIntent.CommunityToggled -> editFilters { copy(communities = communities.toggle(intent.value)) }
            is SearchIntent.MinimumPostsChanged -> reduce { copy(minPostsInput = intent.value, validationMessage = null) }
            is SearchIntent.ObservedAfterChanged -> reduce { copy(observedAfterInput = intent.value, validationMessage = null) }
            is SearchIntent.SortSelected -> editFilters { copy(sort = intent.value) }
            SearchIntent.ApplyFilters -> applyFilters()
            SearchIntent.ResetFilters -> reduce {
                copy(filterDraft = OpportunitySearchQuery(query = draftQuery.query), minPostsInput = "1",
                    observedAfterInput = "", validationMessage = null)
            }
            is SearchIntent.QueryChanged -> reduce { copy(draftQuery = draftQuery.copy(query = intent.value), validationMessage = null) }
            is SearchIntent.AssistantChanged -> reduce { copy(assistantInput = intent.value, validationMessage = null) }
            is SearchIntent.FiltersChanged -> reduce { copy(draftQuery = intent.value, validationMessage = null) }
            SearchIntent.Submit -> submit(state.value.draftQuery)
            SearchIntent.Reload -> reload()
            SearchIntent.LoadNextPage, SearchIntent.RetryNextPage -> runRequest(append = true)
            SearchIntent.InterpretQuery -> interpret()
            SearchIntent.ConfirmDraft -> confirmDraft()
            is SearchIntent.WatchCluster -> watch(intent.clusterId)
            is SearchIntent.OpenCluster -> sendEffect(SearchEffect.NavigateToCluster(intent.clusterId))
        }
    }

    private fun enter() {
        if (entered) return
        entered = true
        val restored = savedState.get<Bundle>("submittedSearch")?.let { bundle ->
            runCatching {
                OpportunitySearchQuery(
                    query = bundle.getString("query").orEmpty(),
                    sources = bundle.getStringArrayList("sources").orEmpty(),
                    communities = bundle.getStringArrayList("communities").orEmpty(),
                    minPosts = bundle.getInt("minimum", 1),
                    observedAfter = bundle.getString("after")?.let(Instant::parse),
                    sort = OpportunitySearchSort.valueOf(bundle.getString("sort") ?: "RELEVANCE"),
                    limit = bundle.getInt("limit", 20),
                ).normalized()
            }.getOrNull()
        }
        restored?.let(::submit)
    }

    private fun submit(input: OpportunitySearchQuery): Boolean {
        val query = runCatching { input.normalized() }.getOrNull()
        if (query == null || query == OpportunitySearchQuery()) {
            reduce { copy(validationMessage = R.string.search_invalid_query) }
            return false
        }
        reduce { copy(draftQuery = query, validationMessage = null) }
        if (state.value.submittedQuery == query) {
            reload()
            return true
        }
        savedState["submittedSearch"] = Bundle().apply {
            putString("query", query.query)
            putStringArrayList("sources", ArrayList(query.sources))
            putStringArrayList("communities", ArrayList(query.communities))
            putInt("minimum", query.minPosts)
            putString("after", query.observedAfter?.toString())
            putString("sort", query.sort.name)
            putInt("limit", query.limit)
        }
        observation?.cancel()
        invalidateRequests()
        reduce {
            copy(submittedQuery = query, userId = null, items = emptyList(), total = 0, cacheReady = false,
                hasSynced = false, canLoadMore = false, pendingClusterIds = emptySet(),
                observationRequest = RequestState.Loading, refreshRequest = RequestState.Idle,
                pageRequest = RequestState.Idle, interpretation = null, interpretationRequest = RequestState.Idle)
        }
        startObserving(query)
        return true
    }

    private fun startObserving(query: OpportunitySearchQuery) {
        if (observation?.isActive == true) return
        var refreshAfterRead = true
        reduce { copy(observationRequest = RequestState.Loading, cacheReady = false) }
        observation = safeLaunch {
            observeSearch(query).collect { snapshot ->
                if (snapshot.query != state.value.submittedQuery) return@collect
                val previous = state.value.userId
                val changed = previous != snapshot.userId
                if (changed) {
                    invalidateRequests()
                    refreshAfterRead = true
                }
                reduce {
                    copy(
                        userId = snapshot.userId, items = snapshot.items, total = snapshot.total,
                        cacheReady = snapshot.cacheReady, hasSynced = snapshot.hasSynced,
                        canLoadMore = snapshot.nextOffset != null,
                        observationRequest = if (snapshot.cacheReady) RequestState.Success(Unit) else RequestState.Loading,
                        refreshRequest = if (changed) RequestState.Idle else refreshRequest,
                        pageRequest = if (changed) RequestState.Idle else pageRequest,
                        pendingClusterIds = if (changed) emptySet() else pendingClusterIds,
                        interpretation = if (changed) null else interpretation,
                        interpretationRequest = if (changed) RequestState.Idle else interpretationRequest,
                    )
                }
                if (snapshot.cacheReady && snapshot.userId != null && refreshAfterRead) {
                    refreshAfterRead = false
                    runRequest(append = false)
                }
            }
        }
    }

    private fun reload() {
        val query = state.value.submittedQuery ?: return
        if (state.value.observationRequest is RequestState.Error || observation?.isActive != true) {
            startObserving(query)
        } else runRequest(append = false)
    }

    private fun runRequest(append: Boolean) {
        val current = state.value
        val query = current.submittedQuery ?: return
        if (current.userId == null || !current.cacheReady || current.observationRequest !is RequestState.Success ||
            current.refreshRequest is RequestState.Loading || current.pageRequest is RequestState.Loading ||
            append && (!current.hasSynced || !current.canLoadMore)
        ) return
        val version = generation
        setRequest(append, RequestState.Loading)
        request = safeLaunch {
            try {
                if (append) loadNextPage(query) else refreshSearch(query)
                if (version == generation) {
                    setRequest(append, RequestState.Success(Unit))
                    if (!append) reduce { copy(pageRequest = RequestState.Idle) }
                }
            } catch (cancelled: CancellationException) {
                if (version == generation) setRequest(append, RequestState.Idle)
                throw cancelled
            } catch (error: Exception) {
                if (version == generation) setRequest(append, RequestState.Error(error.failure()))
            }
        }
    }

    private fun setRequest(append: Boolean, value: RequestState<Unit, OpportunityFailure>) =
        reduce { if (append) copy(pageRequest = value) else copy(refreshRequest = value) }

    private fun watch(clusterId: Int) {
        val current = state.value
        val user = current.userId ?: return
        val card = current.items.firstOrNull { it.id == clusterId } ?: return
        if (current.observationRequest !is RequestState.Success || clusterId in current.pendingClusterIds) return
        val version = generation
        reduce { copy(pendingClusterIds = pendingClusterIds + clusterId) }
        val job = safeLaunch {
            try {
                setWatched(SetOpportunityWatchedUseCase.Params(user, clusterId, !card.isWatched))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (version == generation) sendEffect(SearchEffect.ActionFailed(error.failure()))
            } finally {
                if (version == generation) reduce { copy(pendingClusterIds = pendingClusterIds - clusterId) }
            }
        }
        watches[clusterId] = job
        job.invokeOnCompletion { if (watches[clusterId] === job) watches.remove(clusterId) }
    }

    private fun interpret() {
        val current = state.value
        if (current.interpretationRequest is RequestState.Loading) return
        val text = current.assistantInput.trim()
        if (text.length !in 2..2_000) {
            reduce { copy(validationMessage = R.string.search_invalid_assistant) }
            return
        }
        val version = ++assistantVersion
        reduce { copy(interpretationRequest = RequestState.Loading, validationMessage = null) }
        interpretation = safeLaunch {
            try {
                val result = prepareDraft(PrepareOpportunitySearchDraftUseCase.Params(text, current.interpretation?.query))
                if (version == assistantVersion) reduce {
                    copy(interpretation = result, interpretationRequest = RequestState.Success(Unit),
                        assistantInput = if (assistantInput == current.assistantInput) "" else assistantInput)
                }
            } catch (cancelled: CancellationException) {
                if (version == assistantVersion) reduce { copy(interpretationRequest = RequestState.Idle) }
                throw cancelled
            } catch (error: Exception) {
                if (version == assistantVersion) reduce { copy(interpretationRequest = RequestState.Error(error.failure())) }
            }
        }
    }

    private fun confirmDraft() {
        val current = state.value
        val draft = current.interpretation ?: return
        if (draft.needsClarification || current.interpretationRequest !is RequestState.Success) return
        if (submit(draft.query)) {
            reduce { copy(interpretation = null, interpretationRequest = RequestState.Idle, assistantInput = "") }
            sendEffect(SearchEffect.NavigateBack)
        }
    }

    private fun openFilters() {
        reduce {
            copy(filterDraft = draftQuery, minPostsInput = draftQuery.minPosts.toString(),
                observedAfterInput = draftQuery.observedAfter?.toString()?.substringBefore('T').orEmpty(),
                validationMessage = null)
        }
        fetchOptions()
        sendEffect(SearchEffect.NavigateToFilters)
    }

    private fun fetchOptions() {
        if (options?.isActive == true) return
        reduce { copy(optionsRequest = RequestState.Loading) }
        options = safeLaunch {
            try {
                val result = loadOptions(Unit)
                reduce { copy(sourceOptions = result.sources, communityOptions = result.communities, optionsRequest = RequestState.Success(Unit)) }
            } catch (cancelled: CancellationException) {
                reduce { copy(optionsRequest = RequestState.Idle) }
                throw cancelled
            } catch (error: Exception) {
                reduce { copy(optionsRequest = RequestState.Error(error.failure())) }
            }
        }
    }

    private fun editFilters(edit: OpportunitySearchQuery.() -> OpportunitySearchQuery) =
        reduce { copy(filterDraft = (filterDraft ?: draftQuery).edit(), validationMessage = null) }

    private fun applyFilters() {
        val current = state.value
        val filters = current.filterDraft ?: current.draftQuery
        val input = current.observedAfterInput.trim()
        val edited = runCatching {
            val date = if (input.isEmpty()) null else if (filters.observedAfter?.toString()?.substringBefore('T') == input) {
                filters.observedAfter
            } else {
                Instant.fromEpochSeconds(LocalDate.parse(input).atStartOfDay().toEpochSecond(ZoneOffset.UTC))
            }
            filters.copy(query = "", minPosts = requireNotNull(current.minPostsInput.trim().toIntOrNull()), observedAfter = date)
                .normalized().copy(query = current.draftQuery.query)
        }.getOrNull()
        if (edited == null) {
            reduce { copy(validationMessage = R.string.search_invalid_filters) }
            return
        }
        reduce { copy(draftQuery = edited, filterDraft = null, validationMessage = null) }
        sendEffect(SearchEffect.NavigateBack)
    }

    private fun List<String>.toggle(value: String): List<String> {
        val key = value.trim().lowercase()
        return if (key in this) this - key else this + key
    }

    private fun invalidateRequests() {
        generation++
        assistantVersion++
        request?.cancel()
        interpretation?.cancel()
        watches.values.toList().forEach { it.cancel() }
        watches.clear()
    }

    override fun onError(error: Throwable) {
        invalidateRequests()
        reduce {
            copy(cacheReady = false, observationRequest = RequestState.Error(error.failure()),
                refreshRequest = RequestState.Idle, pageRequest = RequestState.Idle,
                pendingClusterIds = emptySet(), interpretation = null, interpretationRequest = RequestState.Idle)
        }
    }

    private fun Throwable.failure() = (this as? OpportunityOperationException)?.failure ?: OpportunityFailure.UNKNOWN
}
