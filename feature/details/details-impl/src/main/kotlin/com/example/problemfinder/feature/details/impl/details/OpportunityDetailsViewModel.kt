package com.example.problemfinder.feature.details.impl.details

import com.example.problemfinder.core.model.opportunity.OpportunityFailure
import com.example.problemfinder.core.model.opportunity.OpportunityOperationException
import com.example.problemfinder.core.mvi.BaseMVIViewModel
import com.example.problemfinder.core.mvi.RequestState
import com.example.problemfinder.domain.opportunities.api.actions.SetOpportunityWatchedUseCase
import com.example.problemfinder.domain.opportunities.api.details.LoadNextOpportunityEvidenceUseCase
import com.example.problemfinder.domain.opportunities.api.details.ObserveOpportunityDetailsUseCase
import com.example.problemfinder.domain.opportunities.api.details.RefreshOpportunityDetailsUseCase
import com.example.problemfinder.feature.details.impl.util.OpportunityDetailsUIError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import javax.inject.Inject

@HiltViewModel
class OpportunityDetailsViewModel @Inject constructor(
    private val observeDetails: ObserveOpportunityDetailsUseCase,
    private val refreshDetails: RefreshOpportunityDetailsUseCase,
    private val loadNextEvidence: LoadNextOpportunityEvidenceUseCase,
    private val setWatched: SetOpportunityWatchedUseCase,
) : BaseMVIViewModel<OpportunityDetailsIntent, OpportunityDetailsState, OpportunityDetailsEffect>(
    OpportunityDetailsState()
) {

    private var needsInitialRefresh = true

    private var observationJob: Job? = null
    private var activeId: Int = -1

    private var refreshJob: Job? = null
    private var refreshVersion = 0L

    private var loadNextJob: Job? = null
    private var loadVersion = 0L

    private var watchJobs: MutableMap<Int, Job> = mutableMapOf()
    private var watchVersion = 0L

    override fun onIntent(intent: OpportunityDetailsIntent) {
        when (intent) {
            is OpportunityDetailsIntent.Entered -> startObserving(intent.clusterId)
            is OpportunityDetailsIntent.OpenEvidence -> openEvidence(intent.evidenceId)
            OpportunityDetailsIntent.OpenEvidenceList -> sendEffect(OpportunityDetailsEffect.NavigateToEvidenceList)
            OpportunityDetailsIntent.Reload -> reload()
            OpportunityDetailsIntent.LoadNextEvidencePage,
            OpportunityDetailsIntent.RetryEvidencePage -> loadMore()

            OpportunityDetailsIntent.WatchClicked -> watch()
            OpportunityDetailsIntent.BackClicked -> sendEffect(OpportunityDetailsEffect.NavigateBack)
        }
    }

    private fun startObserving(clusterId: Int) {
        if (clusterId <= 0) return onError(
            OpportunityOperationException(
                OpportunityFailure.CLUSTER_NOT_FOUND,
                Exception()
            )
        )

        if (observationJob?.isActive == true && clusterId == state.value.clusterId) return
        // REVIEW [DETAILS-1] Contract decision: Navigation currently gives each Details entry its
        // own VM. Should this public Entered intent ALSO support selecting 43 after cached 42 in
        // the SAME VM? If yes, trace the copy below and needsInitialRefresh on the same account:
        // which content/request flags still belong to 42 when 43's priming snapshot arrives?
        // Check the chosen contract explicitly: either support that transition without stale content
        // or enforce one cluster per VM. Re-entering the same observed ID must not duplicate work.
        cancelJobs()

        observationJob = safeLaunch {
            reduce {
                copy(clusterId = clusterId)
            }

            observeDetails(ObserveOpportunityDetailsUseCase.Params(clusterId)).collect { response ->
                if (state.value.userId != response.userId) {
                    // REVIEW [DETAILS-11] also applies here: which outstanding operation is absent
                    // from this account-change cancellation? Its UI completion has its own lifetime.
                    cancelWatch()
                    cancelRefresh()
                    needsInitialRefresh = true
                }

                reduce {
                    val current = if (userId == response.userId) {
                        this
                    } else {
                        OpportunityDetailsState(userId = response.userId)
                    }

                    if (response.cacheReady.not()) {
                        current.copy(
                            cacheReady = false,
                            clusterId = clusterId,
                            observationRequest = RequestState.Loading
                        )
                    } else {
                        current.copy(
                            details = response.details,
                            clusterId = clusterId,
                            cachedOpportunity = response.cachedOpportunity,
                            topEvidence = response.topEvidence,
                            posts = response.posts,
                            cacheReady = response.cacheReady,
                            hasSynced = response.hasSynced,
                            canLoadMore = (response.pageSize * response.lastLoadedPage) < response.totalPosts,
                            observationRequest = RequestState.Success(Unit)
                        )
                    }
                }

                if (response.cacheReady && response.userId != null && needsInitialRefresh) {
                    needsInitialRefresh = false
                    reload()
                }

            }
        }


    }

    private fun reload() {
        // REVIEW [DETAILS-2] Retry: make the Room collector throw before its first completed read.
        // The screen sends Reload, but cacheReady is still false. Trace the guard below.
        // Now fail the collector AFTER a successful read: can a successful HTTP refresh restart it?
        // Decide how Retry restores observation as well as networking, preserving usable cached
        // content for the same account/cluster. Check both failure timings, not just HTTP failure.
        val current = state.value

        if ((current.clusterId == null) || (current.clusterId <= 0))
            return onError(
                OpportunityOperationException(
                    OpportunityFailure.CLUSTER_NOT_FOUND,
                    Exception()
                )
            )

        if (
            current.userId == null ||
            current.cacheReady.not() ||
            current.refreshRequest is RequestState.Loading
        ) return

        // REVIEW [DETAILS-3] Overlap: Load more refuses to start during refresh, but what happens
        // when Refresh is tapped during page 2? The repository mutex serializes writes, not UI state.
        // Choose the refresh-versus-append policy and trace both completion orders, including failure.
        // Check: the final posts, page indicator and refresh indicator describe the same context.
        refreshVersion += 1L
        val version = refreshVersion

        reduce { copy(refreshRequest = RequestState.Loading) }

        refreshJob = safeLaunch {
            try {
                refreshDetails(
                    RefreshOpportunityDetailsUseCase.Params(
                        clusterId = current.clusterId
                    )
                )

                if (version == refreshVersion) {
                    reduce { copy(refreshRequest = RequestState.Success(Unit)) }
                }
            } catch (cancelled: CancellationException) {
                if (version == refreshVersion) {
                    reduce { copy(refreshRequest = RequestState.Idle) }
                }
                throw cancelled
            } catch (error: Exception) {
                val failure =
                    (error as? OpportunityOperationException)?.failure ?: OpportunityFailure.UNKNOWN

                if (version == refreshVersion) {
                    reduce { copy(refreshRequest = RequestState.Error(OpportunityDetailsUIError(failure))) }
                }
            }
        }
    }

    private fun loadMore() {
        val current = state.value
        val clusterId = current.clusterId

        if (clusterId == null || clusterId <= 0) return onError(
            OpportunityOperationException(
                OpportunityFailure.CLUSTER_NOT_FOUND,
                Exception()
            )
        )

        // REVIEW [DETAILS-4] Admission: send LoadNextEvidencePage twice before Room emits again.
        // Which guard currently notices the first page job? Where does pageRequest become Loading?
        // Also compare cacheReady with hasSynced: does finishing a local read prove page 1 is saved?
        // Check: one in-flight append, and no append before a successfully synced parent exists.
        if (
            current.userId.isNullOrEmpty() ||
            current.canLoadMore.not() ||
            current.cacheReady.not() ||
            refreshJob?.isActive == true
        ) return

        loadVersion += 1L
        val version = loadVersion

        // REVIEW [DETAILS-5] Timeline: mark when safeLaunch RETURNS a Job, when the suspended use
        // case finishes, and when the Success below executes. Are those the same moment?
        // If the use case throws later, which catch receives it: these catches or BaseMVIViewModel?
        // Work out which coroutine should own the full page-request lifecycle before rearranging it.
        // Check: a waiting page stays Loading; failure preserves posts and exposes the footer retry.
        try {
            loadNextJob = safeLaunch {
                loadNextEvidence(LoadNextOpportunityEvidenceUseCase.Params(clusterId))
            }

            if (loadVersion == version) {
                reduce {
                    copy(
                        pageRequest = RequestState.Success(Unit)
                    )
                }
            }
        } catch (cancelled: CancellationException) {
            if (loadVersion == version) {
                reduce {
                    copy(
                        pageRequest = RequestState.Idle
                    )
                }
            }
            throw cancelled
        } catch (exception: Exception) {
            val failure = (exception as? OpportunityOperationException)?.failure ?: OpportunityFailure.UNKNOWN
            if (loadVersion == version) {
                reduce {
                    copy(
                        pageRequest = RequestState.Error(OpportunityDetailsUIError(failure)),
                    )
                }
            }
        }
    }

    private fun watch() {
        val current = state.value
        val opportunity = current.cachedOpportunity
        // REVIEW [DETAILS-6] Boolean trace: try an unwatched opportunity, then a watched one.
        // For each, follow this guard AND the watched argument passed to the use case below.
        // Can the first become watched and the second become unwatched? Write the two desired
        // before/after pairs before changing either expression; unknown membership is a third case.
        if (
            current.userId == null ||
            current.clusterId == null ||
            opportunity == null ||
            opportunity.isWatched.not() ||
            current.cacheReady.not() ||
            refreshJob?.isActive == true ||
            loadNextJob?.isActive == true
        ) return

        // REVIEW [DETAILS-7] Repeated intent: the button disables after state is rendered, but does
        // this function itself reject a second WatchClicked while its first job is suspended?
        // Check one server mutation per opportunity while pending, including direct ViewModel intents.
        reduce { copy(isWatchPending = true) }

        // REVIEW [DETAILS-8] Ownership: what distinguishes two requests if each assigns version 1?
        // Trace cancelWatch(), a new request, and a delayed completion from the old request.
        // Decide what must stay unique and what must still match before that completion changes UI.
        watchVersion = 1L
        val version = watchVersion

        val job = safeLaunch {
            try {
                setWatched(
                    SetOpportunityWatchedUseCase.Params(
                        userId = current.userId,
                        clusterId = current.clusterId,
                        watched = opportunity.isWatched
                    )
                )

                if (watchVersion == version) {
                    // REVIEW [DETAILS-9] Terminal states: who clears isWatchPending on success?
                    // Removing a Job from watchJobs does not change screen state. Audit failure and
                    // cancellation too. Check: pending ends for this request without an older job
                    // clearing a newer request's spinner; confirmed membership still comes from Room.
                }
            } catch (cancelled: CancellationException) {
                // REVIEW [DETAILS-10] Follow the owner of this operation through BOTH catch blocks.
                // Why are they comparing loadVersion and changing pageRequest inside a watch job?
                // Check: failed Remove leaves evidence paging alone and gives action feedback.
                // Hint: inspect the existing ActionFailed effect and its navigation consumer.
                if (loadVersion == version) {
                    reduce {
                        copy(
                            pageRequest = RequestState.Idle
                        )
                    }
                }
                throw cancelled
            } catch (exception: Exception) {
                val failure = (exception as? OpportunityOperationException)?.failure
                    ?: OpportunityFailure.UNKNOWN
                if (loadVersion == version) {
                    reduce {
                        copy(
                            pageRequest = RequestState.Error(OpportunityDetailsUIError(failure)),
                        )
                    }
                }
            }
        }
        watchJobs[opportunity.id] = job
        job.invokeOnCompletion {
            if(watchJobs[current.clusterId] == job)
                watchJobs.remove(current.clusterId)
        }
    }

    private fun openEvidence(evidenceId: Int) = sendEffect(
        OpportunityDetailsEffect.NavigateToEvidence(evidenceId)
    )

    override fun onError(error: Throwable) {
        // REVIEW [DETAILS-2/5/11] Once operation-specific errors are handled by their owners, trace
        // a genuine observation failure here. What happens to active page/watch jobs, and how can
        // the Retry button recover the stopped stream rather than only clearing an HTTP indicator?
        cancelRefresh()

        val failure = (error as? OpportunityOperationException)?.failure ?: OpportunityFailure.UNKNOWN

        reduce {
            copy(
                observationRequest = RequestState.Error(OpportunityDetailsUIError(failure)),
                refreshRequest = RequestState.Idle,
            )
        }
    }

    private fun cancelJobs() {
        // REVIEW [DETAILS-11] Cancellation inventory: observation, refresh, page, watch.
        // Which Job/version pair is not represented here? Trace an old page request failing after
        // Entered selects another cluster or the observed account changes. Repository account checks
        // protect cache writes; they do not own this ViewModel's request indicators.
        // Check every context-change/error path and the Loading/pending state left behind afterward.
        cancelObserve()
        cancelWatch()
        cancelRefresh()
    }

    private fun cancelObserve() {
        activeId = -1
        observationJob?.cancel()
        observationJob = null
    }

    private fun cancelWatch() {
        watchVersion += 1L
        watchJobs.values.toList().forEach { it.cancel() }
        watchJobs.clear()
    }

    private fun cancelRefresh() {
        refreshVersion += 1L
        refreshJob?.cancel()
        refreshJob = null
    }
}
