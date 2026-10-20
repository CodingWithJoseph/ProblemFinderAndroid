package com.example.problemfinder.feature.search.impl

import com.example.problemfinder.core.mvi.BaseMVIViewModel
import com.example.problemfinder.domain.opportunities.api.actions.SetOpportunityWatchedUseCase
import com.example.problemfinder.domain.opportunities.api.search.LoadNextOpportunitySearchPageUseCase
import com.example.problemfinder.domain.opportunities.api.search.ObserveOpportunitySearchUseCase
import com.example.problemfinder.domain.opportunities.api.search.PrepareOpportunitySearchDraftUseCase
import com.example.problemfinder.domain.opportunities.api.search.RefreshOpportunitySearchUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** Input/navigation wiring is supplied. SEARCH-5 is the actual result/request lifecycle to write. */
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val observeSearch: ObserveOpportunitySearchUseCase,
    private val refreshSearch: RefreshOpportunitySearchUseCase,
    private val loadNextPage: LoadNextOpportunitySearchPageUseCase,
    private val prepareDraft: PrepareOpportunitySearchDraftUseCase,
    private val setWatched: SetOpportunityWatchedUseCase,
) : BaseMVIViewModel<SearchIntent, SearchState, SearchEffect>(SearchState()) {
    override fun onIntent(intent: SearchIntent) {
        when (intent) {
            SearchIntent.Entered -> onEntered()
            is SearchIntent.QueryChanged -> reduce { copy(draftQuery = draftQuery.copy(query = intent.value)) }
            is SearchIntent.FiltersChanged -> reduce { copy(draftQuery = intent.value) }
            SearchIntent.Submit -> submit()
            SearchIntent.Reload -> reload()
            SearchIntent.LoadNextPage, SearchIntent.RetryNextPage -> loadMore()
            SearchIntent.InterpretQuery -> interpret()
            SearchIntent.ConfirmDraft -> confirmDraft()
            is SearchIntent.WatchCluster -> watch(intent.clusterId)
            is SearchIntent.OpenCluster -> sendEffect(SearchEffect.NavigateToCluster(intent.clusterId))
        }
    }

    private fun onEntered() {
        // TODO [SEARCH-5A] Restore an existing result context; a fresh tab does not run an empty search.
        // If this ViewModel already retains submittedQuery, reuse its observer/state when returning from
        // Details. For process restoration, save only small normalized query/filter state in SavedStateHandle,
        // then reopen the corresponding Room cache. Large result lists remain in Room, not saved arguments.
        // With no submitted query, stay in an initial "enter a query" state, distinct from synced zero results.
        // Finish check: entering a fresh Search tab issues no request; Back from Details retains query/results.
    }

    private fun submit() {
        // TODO [SEARCH-5A] Capture a new result-set identity without coupling it to later text editing.
        // 1. Validate/normalize draftQuery. Represent input validation separately from network/cache errors
        //    and localize the displayed message in Screen. Invalid input starts no observer/request.
        // 2. Capture an immutable submittedQuery, cancel old observation/refresh/page/interpretation jobs
        //    as appropriate, and increment a query generation before replacing the selected query's state.
        // 3. Collect observeSearch(query). Accept only snapshots matching the active query/account/generation.
        //    Reduce content/total/paging/cache metadata from snapshots, clearing prior account/query content.
        // 4. After the first actual cache read for a known account, issue ONE initial refresh. Room emissions
        //    from paging or watch changes must not start another automatic refresh each time.
        // 5. Keep draftQuery editable without modifying submittedQuery. Refresh/paging use submittedQuery.
        // Retain the result context/scroll on Back from Details. Reference BrowseViewModel's query/account
        // guards and DiscoveryViewModel's initial-refresh pattern; replace cursor assumptions with offsets.
        // Finish check: A completes after B submission -> B remains displayed; simply typing B doesn't fetch.
    }

    private fun reload() {
        // TODO [SEARCH-5B] Refresh the currently SUBMITTED query, not whatever text is being edited.
        // Require a known account, selected submitted query and healthy cache observer; recover observation
        // first if it failed. Ignore duplicate refreshes. Capture query/account/generation, set refreshRequest
        // Loading and call refreshSearch(query) in a retained Job. Handle its success/error/cancellation only
        // when the capture still matches. The snapshot collector updates cards; this function only finishes
        // the request indicator. Keep displayed cache data through network failure and offer retry.
        // Finish check: shown query A + edited text B -> Refresh still refreshes A; late A cannot stop B's spinner.
    }

    private fun loadMore() {
        // TODO [SEARCH-5C] Append only the current synced result set.
        // Require submittedQuery/userId/hasSynced/canLoadMore and no active append or replacement refresh.
        // Capture query/account/generation, set pageRequest Loading and call loadNextPage(submittedQuery).
        // The repo owns offset selection. Catch page errors here, retain all existing results and expose
        // a footer RetryNextPage action. Re-check the capture before every completion/finally state update.
        // Finish check: duplicate taps create one append; A's cancelled append cannot alter B's data/indicator.
    }

    private fun watch(clusterId: Int) {
        // TODO [SEARCH-5D] Update membership through the same use case used by Details/Discovery/Browse.
        // Find the result in current cached items; require userId and that its ID is not already pending.
        // Capture desired state (!isWatched), query/account generation and a per-ID Job. Add its ID to the
        // pending set before calling setWatched. Room emits the confirmed value; do not flip it optimistically.
        // Emit ActionFailed on ordinary failure and clear pending only for the same active account/generation.
        // Cancel pending work on account changes; suppress stale effects. Query changes also need a deliberate
        // pending-job policy so the same visible ID cannot start competing actions while an old job survives.
        // Finish check: Add affects the shared card everywhere; a failed watch leaves result content intact.
    }

    private fun interpret() {
        // TODO [SEARCH-8] Call prepareDraft with the message and optional existing draft; reduce
        // interpretationRequest and interpretation only. A clarification needs another user turn.
        // Never overwrite submittedQuery or issue a result request merely because a draft arrived.
        // Server fallback_used is displayed as a usable draft; backend handles model availability.
    }

    private fun confirmDraft() {
        // TODO [SEARCH-8] Require a draft with needsClarification=false, copy its normalized query
        // into draftQuery, then use the SAME submit path as manual Search. Explicit confirmation
        // is the boundary between interpretation and database execution. Clear obsolete drafts.
    }

    override fun onError(error: Throwable) {
        // TODO [SEARCH-5e] Handle refresh/page/watch/interpretation exceptions within those jobs.
        // Reserve this fallback for observation/unhandled failures, map typed OpportunityFailure,
        // cancel dependent work, keep useful content and make retry possible. Preserve cancellation.
        // Check: a failed assistant request cannot erase a successful manual result list.
    }
}
