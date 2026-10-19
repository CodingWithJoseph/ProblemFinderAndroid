package com.example.problemfinder.feature.opportunities.impl.browse

import androidx.lifecycle.SavedStateHandle
import com.example.problemfinder.domain.opportunities.api.actions.SetOpportunityWatchedUseCase
import androidx.lifecycle.ViewModelStore
import com.example.problemfinder.core.common.shared.OpportunityCategoryFilter
import com.example.problemfinder.core.common.shared.SortType
import com.example.problemfinder.core.model.opportunity.OpportunityFailure
import com.example.problemfinder.core.model.opportunity.OpportunityOperationException
import com.example.problemfinder.core.model.browse.BrowseSnapshot
import com.example.problemfinder.core.model.opportunity.Opportunity
import com.example.problemfinder.core.mvi.RequestState
import com.example.problemfinder.domain.opportunities.api.browse.LoadNextBrowsePageUseCase
import com.example.problemfinder.domain.opportunities.api.browse.ObserveBrowseUseCase
import com.example.problemfinder.domain.opportunities.api.browse.RefreshBrowseUseCase
import com.example.problemfinder.feature.opportunities.api.OpportunitiesDestinations
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.coroutines.cancellation.CancellationException

@OptIn(ExperimentalCoroutinesApi::class)
class BrowseViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val observe = FakeObserve()
    private val actions = FakeActions()
    private val refresh = FakeRefresh()
    private val pages = FakePages()
    private val refreshCalls get() = refresh.operation.calls
    private val pageCalls get() = pages.operation.calls

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        // Release deliberately cancellation-resistant requests before clearing the VM.
        refreshCalls.forEach { it.response.complete(Unit) }
        pageCalls.forEach { it.response.complete(Unit) }
        store.clear()
        dispatcher.scheduler.runCurrent()
        Dispatchers.resetMain()
    }

    @Test
    fun watchActionDisablesOnlyItsOpportunityAndIgnoresDuplicateTaps() = runTest {
        val model = createModel()
        publish(ready(listOf(1, 2)))
        finishRefresh()
        model.onIntent(BrowseIntent.WatchCluster(1))
        model.onIntent(BrowseIntent.WatchCluster(1))
        model.onIntent(BrowseIntent.WatchCluster(2))
        runCurrent()
        assertEquals(2, actions.calls.size)
        assertEquals(SetOpportunityWatchedUseCase.Params(USER, 1, true), actions.calls.first().params)
        assertEquals(setOf(1, 2), model.state.value.pendingClusterIds)
        actions.calls.first().response.complete(Unit)
        runCurrent()
        assertEquals(setOf(2), model.state.value.pendingClusterIds)
        actions.calls.last().response.complete(Unit)
        runCurrent()
        assertTrue(model.state.value.pendingClusterIds.isEmpty())
    }

    @Test
    fun failedWatchPreservesContentAndShowsAnActionError() = runTest {
        val model = createModel()
        publish(ready(listOf(1)))
        finishRefresh()
        model.onIntent(BrowseIntent.WatchCluster(1))
        runCurrent()
        actions.calls.single().response.completeExceptionally(OpportunityOperationException(OpportunityFailure.NETWORK, java.io.IOException("Offline")))
        runCurrent()
        assertTrue(model.state.value.pendingClusterIds.isEmpty())
        assertFalse(model.state.value.data.single().isWatched)
        assertEquals(RequestState.Success(Unit), model.state.value.refreshRequest)
        assertEquals(BrowseEffect.ShowError(com.example.problemfinder.feature.opportunities.impl.util.OpportunityUiError(OpportunityFailure.NETWORK)), model.effects.first())
    }

    @Test
    fun accountSwitchClearsPendingActionsAndDoesNotReportTheirStaleFailure() = runTest {
        val model = createModel()
        publish(ready(listOf(1)))
        finishRefresh()
        model.onIntent(BrowseIntent.WatchCluster(1))
        runCurrent()
        publish(ready(listOf(2), user = OTHER_USER))
        assertTrue(model.state.value.pendingClusterIds.isEmpty())
        assertEquals(OTHER_USER, model.state.value.userId)
        assertEquals(listOf(2), model.state.value.data.map { it.id })
        finishRefresh()
    }

    @Test
    fun openClusterEmitsSelectedId() = runTest {
        val vm = createModel()
        vm.onIntent(BrowseIntent.OpenCluster(42))
        runCurrent()
        assertEquals(BrowseEffect.NavigateToCluster(42), vm.effects.first())
    }

    @Test
    fun buildCategoryRouteStartsWithItsOwnCacheAndRequests() = runTest {
        assertInitialCategory("BUILD_AND_TOOLING", OpportunityCategoryFilter.BUILD_AND_TOOLING)
    }

    @Test
    fun dataCategoryRouteStartsWithItsOwnCacheAndRequests() = runTest {
        assertInitialCategory("DATA_AND_APIS", OpportunityCategoryFilter.DATA_AND_APIS)
    }

    @Test
    fun workflowCategoryRouteStartsWithItsOwnCacheAndRequests() = runTest {
        assertInitialCategory("WORKFLOW_AND_AUTOMATION", OpportunityCategoryFilter.WORKFLOW_AND_AUTOMATION)
    }

    @Test
    fun uncategorizedRouteDoesNotBecomeAnAllCategoriesRequest() = runTest {
        assertInitialCategory("UNCATEGORIZED", OpportunityCategoryFilter.UNCATEGORIZED)
    }

    @Test
    fun explicitAllCategoryRouteUsesAllCategories() = runTest {
        assertInitialCategory("ALL", OpportunityCategoryFilter.ALL)
    }

    @Test
    fun unrecognizedRouteCategoryFallsBackToAll() = runTest {
        assertInitialCategory("not-a-category", OpportunityCategoryFilter.ALL)
    }

    @Test
    fun waitsForActualCacheReadAndPublishesCachedRowsBeforeStartingRefresh() = runTest {
        val model = createModel()
        assertEquals(1, observe.calls.size)
        assertFalse(model.state.value.cacheReady)
        assertTrue(refreshCalls.isEmpty())

        publish(BrowseSnapshot(userId = USER, cacheReady = false))
        assertEquals(USER, model.state.value.userId)
        assertFalse(model.state.value.cacheReady)
        assertTrue(refreshCalls.isEmpty())

        var stateAtRequest: BrowseState? = null
        refresh.operation.onStart = { stateAtRequest = model.state.value }
        publish(ready(listOf(8, 3)))

        assertEquals(listOf(8, 3), stateAtRequest?.data?.map { it.id })
        assertTrue(model.state.value.cacheReady)
        assertTrue(model.state.value.hasSynced)
        assertEquals(RequestState.Loading, model.state.value.refreshRequest)
        assertEquals(1, refreshCalls.size)
        assertEquals(RefreshBrowseUseCase.Params(OpportunityCategoryFilter.ALL, SortType.NEWEST), refreshCalls.single().params)
    }

    @Test
    fun failedFirstSyncRemainsUnsyncedAndCanBeRetried() = runTest {
        val model = createModel()
        publish(ready(emptyList(), null, synced = false))
        refreshCalls.single().response.completeExceptionally(failure(OpportunityFailure.NETWORK))
        runCurrent()

        assertTrue(model.state.value.data.isEmpty())
        assertFalse(model.state.value.hasSynced)
        assertEquals(OpportunityFailure.NETWORK, model.state.value.contentError?.failure)
        assertTrue(model.state.value.canRefresh)
        assertFalse(model.state.value.canLoadNextPage)

        model.onIntent(BrowseIntent.Reload)
        runCurrent()
        assertEquals(2, refreshCalls.size)
        assertNull(model.state.value.contentError)
    }

    @Test
    fun successfulSyncDoesNotInventDataBeforeObservationReportsIt() = runTest {
        val model = createModel()
        publish(ready(emptyList(), null, synced = false))
        finishRefresh()

        assertTrue(model.state.value.data.isEmpty())
        assertFalse(model.state.value.hasSynced)
        assertEquals(RequestState.Success(Unit), model.state.value.refreshRequest)

        publish(ready(listOf(4), "new-cursor"))
        assertEquals(listOf(4), model.state.value.data.map { it.id })
        assertEquals("new-cursor", model.state.value.cursor)
        assertTrue(model.state.value.hasSynced)
        assertEquals(1, refreshCalls.size)
    }

    @Test
    fun signedOutObservationClearsPresentationAndDoesNotSync() = runTest {
        val model = createModel()
        publish(BrowseSnapshot())
        model.onIntent(BrowseIntent.Reload)
        model.onIntent(BrowseIntent.LoadNextPage)
        runCurrent()

        assertTrue(model.state.value.cacheReady)
        assertNull(model.state.value.userId)
        assertTrue(model.state.value.data.isEmpty())
        assertFalse(model.state.value.canRefresh)
        assertFalse(model.state.value.canLoadNextPage)
        assertTrue(refreshCalls.isEmpty())
        assertTrue(pageCalls.isEmpty())
    }

    @Test
    fun databaseSnapshotsReplaceTheListRatherThanAppendAndDoNotResync() = runTest {
        val model = createModel()
        publish(ready(listOf(1, 2)))
        finishRefresh()
        publish(ready(listOf(9), "replacement-cursor"))

        assertEquals(listOf(9), model.state.value.data.map { it.id })
        assertEquals("replacement-cursor", model.state.value.cursor)
        assertEquals(1, refreshCalls.size)
        assertEquals(1, observe.calls.size)
    }

    @Test
    fun refreshFailurePreservesRowsAndCursorAndRetryClearsOnlyTheError() = runTest {
        val model = createModel()
        publish(ready(listOf(8, 3), "saved-cursor"))
        refreshCalls.single().response.completeExceptionally(failure(OpportunityFailure.SERVER))
        runCurrent()

        assertEquals(listOf(8, 3), model.state.value.data.map { it.id })
        assertEquals("saved-cursor", model.state.value.cursor)
        assertEquals(OpportunityFailure.SERVER, model.state.value.contentError?.failure)

        model.onIntent(BrowseIntent.Reload)
        runCurrent()
        assertEquals(listOf(8, 3), model.state.value.data.map { it.id })
        assertNull(model.state.value.contentError)
        assertEquals(RequestState.Loading, model.state.value.refreshRequest)
        assertEquals(RequestState.Idle, model.state.value.pageRequest)
        assertEquals(1, observe.calls.size)
    }

    @Test
    fun pageFailurePreservesCacheBlocksAutomaticRetryAndAllowsExplicitRetry() = runTest {
        val model = createModel()
        publish(ready())
        finishRefresh()
        model.onIntent(BrowseIntent.LoadNextPage)
        runCurrent()
        pageCalls.single().response.completeExceptionally(failure(OpportunityFailure.NETWORK))
        runCurrent()

        assertEquals(listOf(1), model.state.value.data.map { it.id })
        assertEquals("cursor-1", model.state.value.cursor)
        assertEquals(OpportunityFailure.NETWORK, pageError(model))
        assertNull(model.state.value.contentError)
        assertTrue(model.state.value.canLoadNextPage)
        assertFalse(model.state.value.canAutoLoadNextPage)

        repeat(3) { model.onIntent(BrowseIntent.LoadNextPage) }
        runCurrent()
        assertEquals(1, pageCalls.size)

        model.onIntent(BrowseIntent.RetryNextPage)
        runCurrent()
        assertEquals(2, pageCalls.size)
        assertEquals(RequestState.Loading, model.state.value.pageRequest)
        pageCalls.last().response.complete(Unit)
        runCurrent()
        publish(ready(listOf(1, 2), null))
        assertEquals(listOf(1, 2), model.state.value.data.map { it.id })
        assertFalse(model.state.value.canLoadNextPage)
    }

    @Test
    fun loadingGatesPreventOverlappingRefreshAndPageRequests() = runTest {
        val model = createModel()
        publish(ready())
        repeat(3) {
            model.onIntent(BrowseIntent.Reload)
            model.onIntent(BrowseIntent.LoadNextPage)
            model.onIntent(BrowseIntent.RetryNextPage)
        }
        runCurrent()
        assertEquals(1, refreshCalls.size)
        assertTrue(pageCalls.isEmpty())

        finishRefresh()
        model.onIntent(BrowseIntent.LoadNextPage)
        repeat(3) {
            model.onIntent(BrowseIntent.Reload)
            model.onIntent(BrowseIntent.LoadNextPage)
            model.onIntent(BrowseIntent.RetryNextPage)
        }
        runCurrent()
        assertEquals(1, refreshCalls.size)
        assertEquals(1, pageCalls.size)
        assertEquals(RequestState.Loading, model.state.value.pageRequest)
    }

    @Test
    fun pagingRequiresReadySyncedCacheAndANonNullCursor() = runTest {
        val model = createModel()
        model.onIntent(BrowseIntent.LoadNextPage)
        publish(ready(emptyList(), "cursor-1", synced = false))
        finishRefresh()
        model.onIntent(BrowseIntent.LoadNextPage)
        model.onIntent(BrowseIntent.RetryNextPage)
        publish(ready(listOf(1), null))
        model.onIntent(BrowseIntent.LoadNextPage)
        model.onIntent(BrowseIntent.RetryNextPage)
        runCurrent()

        assertTrue(pageCalls.isEmpty())
        assertFalse(model.state.value.canLoadNextPage)
    }

    @Test
    fun automaticPagingRequestsEachPresentedCursorOnlyOnce() = runTest {
        val model = createModel()
        publish(ready())
        finishRefresh()
        model.onIntent(BrowseIntent.LoadNextPage)
        runCurrent()
        pageCalls.single().response.complete(Unit)
        runCurrent()

        // A completed operation may precede the Room emission advancing the cursor.
        repeat(3) { model.onIntent(BrowseIntent.LoadNextPage) }
        runCurrent()
        assertEquals(1, pageCalls.size)

        publish(ready(listOf(1, 2), "cursor-2"))
        model.onIntent(BrowseIntent.LoadNextPage)
        runCurrent()
        assertEquals(2, pageCalls.size)
        assertEquals(LoadNextBrowsePageUseCase.Params(OpportunityCategoryFilter.ALL, SortType.NEWEST), pageCalls.last().params)
    }

    @Test
    fun refreshResetsPageErrorAndAllowsTheSameCursorToBeRequestedAgain() = runTest {
        val model = createModel()
        publish(ready())
        finishRefresh()
        model.onIntent(BrowseIntent.LoadNextPage)
        runCurrent()
        pageCalls.single().response.completeExceptionally(failure(OpportunityFailure.SERVER))
        runCurrent()

        model.onIntent(BrowseIntent.Reload)
        runCurrent()
        assertEquals(RequestState.Idle, model.state.value.pageRequest)
        finishRefresh()
        model.onIntent(BrowseIntent.LoadNextPage)
        runCurrent()
        assertEquals(2, pageCalls.size)
    }

    @Test
    fun queryChangeCancelsObserverAndPageAndClearsOldQueryMetadata() = runTest {
        val model = createModel()
        publish(ready())
        finishRefresh()
        model.onIntent(BrowseIntent.LoadNextPage)
        runCurrent()
        val oldObservation = observe.calls.single()

        model.onIntent(BrowseIntent.ChangeSortType(SortType.OLDEST))
        runCurrent()

        assertTrue(oldObservation.closed)
        assertTrue(pageCalls.single().cancelled)
        assertEquals(SortType.OLDEST, model.state.value.sort)
        assertTrue(model.state.value.data.isEmpty())
        assertNull(model.state.value.cursor)
        assertFalse(model.state.value.hasSynced)
        assertFalse(model.state.value.cacheReady)
        assertEquals(RequestState.Idle, model.state.value.pageRequest)
        assertEquals(ObserveBrowseUseCase.Params(OpportunityCategoryFilter.ALL, SortType.OLDEST), observe.calls.last().params)

        oldObservation.events.trySend(Event.Value(ready(listOf(99), "stale-cursor")))
        publish(ready(listOf(7), "oldest-cursor"))
        assertEquals(listOf(7), model.state.value.data.map { it.id })
        assertEquals(SortType.OLDEST, refreshCalls.last().params.sort)
        assertEquals("oldest-cursor", model.state.value.cursor)
    }

    @Test
    fun lateRefreshSuccessFromPreviousQueryCannotCompleteTheNewRequest() = runTest {
        refresh.operation.ignoreCancellation = true
        val model = createModel()
        publish(ready())
        val oldRequest = refreshCalls.single()
        model.onIntent(BrowseIntent.ChangeSortType(SortType.OLDEST))
        runCurrent()
        publish(ready(listOf(7), "oldest-cursor"))

        oldRequest.response.complete(Unit)
        runCurrent()
        assertEquals(RequestState.Loading, model.state.value.refreshRequest)
        assertEquals(listOf(7), model.state.value.data.map { it.id })
        assertEquals(SortType.OLDEST, model.state.value.sort)
        finishRefresh()
        assertEquals(RequestState.Success(Unit), model.state.value.refreshRequest)
    }

    @Test
    fun lateRefreshFailureFromPreviousAccountCannotSetTheNewAccountsError() = runTest {
        refresh.operation.ignoreCancellation = true
        val model = createModel()
        publish(ready())
        val oldRequest = refreshCalls.single()
        publish(BrowseSnapshot(userId = OTHER_USER, cacheReady = false))
        assertEquals(OTHER_USER, model.state.value.userId)
        assertTrue(model.state.value.data.isEmpty())
        assertFalse(model.state.value.cacheReady)
        publish(ready(listOf(7), "user-b-cursor", OTHER_USER))

        oldRequest.response.completeExceptionally(failure(OpportunityFailure.SERVER))
        runCurrent()
        assertEquals(OTHER_USER, model.state.value.userId)
        assertEquals(listOf(7), model.state.value.data.map { it.id })
        assertEquals(RequestState.Loading, model.state.value.refreshRequest)
        assertNull(model.state.value.contentError)
        assertEquals(1, observe.calls.size)
    }

    @Test
    fun latePageSuccessCannotChangeTheNewAccountsPageState() = runTest {
        pages.operation.ignoreCancellation = true
        val model = createModel()
        publish(ready())
        finishRefresh()
        model.onIntent(BrowseIntent.LoadNextPage)
        runCurrent()
        val oldPage = pageCalls.single()
        publish(BrowseSnapshot(userId = OTHER_USER, cacheReady = false))
        publish(ready(listOf(7), "user-b-cursor", OTHER_USER))

        oldPage.response.complete(Unit)
        runCurrent()
        assertEquals(RequestState.Idle, model.state.value.pageRequest)
        assertEquals(listOf(7), model.state.value.data.map { it.id })
        assertEquals("user-b-cursor", model.state.value.cursor)
        assertEquals(RequestState.Loading, model.state.value.refreshRequest)
    }

    @Test
    fun signOutCancelsRequestsAndAnotherAccountStartsItsOwnSync() = runTest {
        val model = createModel()
        publish(ready())
        publish(BrowseSnapshot())

        assertTrue(refreshCalls.single().cancelled)
        assertTrue(model.state.value.data.isEmpty())
        assertNull(model.state.value.cursor)
        assertNull(model.state.value.userId)
        assertFalse(model.state.value.canRefresh)
        assertEquals(RequestState.Idle, model.state.value.refreshRequest)

        publish(BrowseSnapshot(userId = OTHER_USER, cacheReady = false))
        assertEquals(1, refreshCalls.size)
        publish(ready(listOf(7), "user-b-cursor", OTHER_USER))
        assertEquals(2, refreshCalls.size)
        assertEquals(listOf(7), model.state.value.data.map { it.id })
    }

    @Test
    fun sameAccountSnapshotsDoNotRestartOrCancelAnActiveRefresh() = runTest {
        val model = createModel()
        publish(ready())
        publish(ready(listOf(9), "changed-cursor"))

        assertEquals(1, refreshCalls.size)
        assertFalse(refreshCalls.single().cancelled)
        assertEquals(RequestState.Loading, model.state.value.refreshRequest)
        assertEquals(listOf(9), model.state.value.data.map { it.id })
    }

    @Test
    fun observationFailureKeepsRowsCancelsPageAndRetryRetainsCacheDuringRead() = runTest {
        val model = createModel()
        publish(ready())
        finishRefresh()
        model.onIntent(BrowseIntent.LoadNextPage)
        runCurrent()
        observe.calls.last().events.trySend(Event.Failed(IllegalStateException("Database unavailable")))
        runCurrent()

        assertTrue(pageCalls.single().cancelled)
        assertEquals(listOf(1), model.state.value.data.map { it.id })
        assertEquals(OpportunityFailure.UNKNOWN, model.state.value.contentError?.failure)
        assertFalse(model.state.value.canLoadNextPage)
        assertTrue(model.state.value.canRefresh)

        model.onIntent(BrowseIntent.Reload)
        runCurrent()
        assertEquals(2, observe.calls.size)
        assertEquals(listOf(1), model.state.value.data.map { it.id })
        assertFalse(model.state.value.cacheReady)
        publish(BrowseSnapshot(userId = USER, cacheReady = false))
        assertEquals(listOf(1), model.state.value.data.map { it.id })
        publish(ready(listOf(9), "recovered-cursor"))
        assertEquals(listOf(9), model.state.value.data.map { it.id })
        assertNull(model.state.value.contentError)
        assertEquals(2, refreshCalls.size)
    }

    @Test
    fun observationFailureBeforeIdentityIsKnownCanBeRetried() = runTest {
        val model = createModel()
        observe.calls.single().events.trySend(Event.Failed(IllegalStateException("Read failed")))
        runCurrent()
        assertTrue(model.state.value.canRefresh)
        assertEquals(OpportunityFailure.UNKNOWN, model.state.value.contentError?.failure)

        model.onIntent(BrowseIntent.Reload)
        runCurrent()
        publish(ready())
        assertEquals(2, observe.calls.size)
        assertEquals(1, refreshCalls.size)
        assertNull(model.state.value.contentError)
    }

    @Test
    fun firstEntryDoesNotDuplicateStartupSyncAndLaterEntryRefreshes() = runTest {
        val model = createModel()
        model.onIntent(BrowseIntent.Entered)
        publish(ready())
        finishRefresh()
        assertEquals(1, refreshCalls.size)

        model.onIntent(BrowseIntent.Entered)
        runCurrent()
        assertEquals(2, refreshCalls.size)
        model.onIntent(BrowseIntent.Entered)
        runCurrent()
        assertEquals(2, refreshCalls.size)
    }

    @Test
    fun selectingTheCurrentQueryDoesNotRestartObservationOrRefresh() = runTest {
        val model = createModel()
        publish(ready())
        finishRefresh()
        model.onIntent(BrowseIntent.ChangeSortType(SortType.NEWEST))
        model.onIntent(BrowseIntent.ChangeCategory(OpportunityCategoryFilter.ALL))
        runCurrent()

        assertEquals(1, observe.calls.size)
        assertEquals(1, refreshCalls.size)
        assertEquals(listOf(1), model.state.value.data.map { it.id })
    }

    @Test
    fun operationCancellationIsNotAUiFailureAndExplicitRetryStillWorks() = runTest {
        val model = createModel()
        publish(ready())
        refreshCalls.single().response.completeExceptionally(CancellationException("Cancelled"))
        runCurrent()
        assertEquals(RequestState.Idle, model.state.value.refreshRequest)
        assertNull(model.state.value.contentError)

        model.onIntent(BrowseIntent.Reload)
        runCurrent()
        finishRefresh()
        model.onIntent(BrowseIntent.LoadNextPage)
        runCurrent()
        pageCalls.single().response.completeExceptionally(CancellationException("Cancelled"))
        runCurrent()
        assertEquals(RequestState.Idle, model.state.value.pageRequest)
        assertNull(model.state.value.contentError)
        assertEquals(listOf(1), model.state.value.data.map { it.id })

        model.onIntent(BrowseIntent.RetryNextPage)
        runCurrent()
        assertEquals(2, pageCalls.size)
    }

    @Test
    fun unclassifiedOperationFailureMapsToUnknownWithoutDroppingCache() = runTest {
        val model = createModel()
        publish(ready())
        refreshCalls.single().response.completeExceptionally(IllegalArgumentException("Unexpected failure"))
        runCurrent()

        assertEquals(OpportunityFailure.UNKNOWN, model.state.value.contentError?.failure)
        assertEquals(listOf(1), model.state.value.data.map { it.id })
        assertEquals("cursor-1", model.state.value.cursor)
    }

    @Test
    fun syncedEmptyCacheRemainsSyncedIfItsRefreshFails() = runTest {
        val model = createModel()
        publish(ready(emptyList(), null))
        refreshCalls.single().response.completeExceptionally(failure(OpportunityFailure.NETWORK))
        runCurrent()

        assertTrue(model.state.value.hasSynced)
        assertTrue(model.state.value.cacheReady)
        assertTrue(model.state.value.data.isEmpty())
        assertEquals(OpportunityFailure.NETWORK, model.state.value.contentError?.failure)
        assertFalse(model.state.value.canLoadNextPage)
    }

    @Test
    fun clearingViewModelCancelsObservationAndItsRequest() = runTest {
        createModel()
        publish(ready())
        store.clear()
        runCurrent()

        assertTrue(observe.calls.single().closed)
        assertTrue(refreshCalls.single().cancelled)
    }

    private fun TestScope.assertInitialCategory(routeCategory: String, expected: OpportunityCategoryFilter) {
        val model = createModel(
            SavedStateHandle(mapOf(OpportunitiesDestinations.CATEGORY_ARGUMENT to routeCategory))
        )
        assertEquals(expected, model.state.value.category)
        assertEquals(
            listOf(ObserveBrowseUseCase.Params(expected, SortType.NEWEST)),
            observe.calls.map { it.params },
        )
        assertTrue(refreshCalls.isEmpty())

        publish(ready())
        assertEquals(
            RefreshBrowseUseCase.Params(expected, SortType.NEWEST),
            refreshCalls.single().params,
        )
        finishRefresh()
        model.onIntent(BrowseIntent.LoadNextPage)
        runCurrent()
        assertEquals(
            LoadNextBrowsePageUseCase.Params(expected, SortType.NEWEST),
            pageCalls.single().params,
        )
        assertEquals(1, observe.calls.size)
    }

    private fun TestScope.createModel(
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
    ): BrowseViewModel {
        val model = BrowseViewModel(observe, refresh, pages, savedStateHandle, actions)
        store.put("browse", model)
        runCurrent()
        return model
    }

    private fun TestScope.publish(snapshot: BrowseSnapshot) {
        check(observe.calls.last().events.trySend(Event.Value(snapshot)).isSuccess)
        runCurrent()
    }

    private fun TestScope.finishRefresh() {
        refreshCalls.last().response.complete(Unit)
        runCurrent()
    }

    private fun pageError(model: BrowseViewModel): OpportunityFailure? =
        (model.state.value.pageRequest as? RequestState.Error)?.error?.failure

    private sealed interface Event {
        data class Value(val snapshot: BrowseSnapshot) : Event
        data class Failed(val error: Exception) : Event
    }

    private class Observation(val params: ObserveBrowseUseCase.Params) {
        val events = Channel<Event>(Channel.UNLIMITED)
        var closed = false
    }

    private class FakeActions : SetOpportunityWatchedUseCase() {
        data class Call(val params: Params, val response: CompletableDeferred<Unit> = CompletableDeferred())
        val calls = mutableListOf<Call>()
        override suspend fun execute(params: Params) {
            val call = Call(params)
            calls += call
            call.response.await()
        }
    }

    private class FakeObserve : ObserveBrowseUseCase() {
        val calls = mutableListOf<Observation>()

        override suspend fun execute(params: Params): Flow<BrowseSnapshot> {
            val call = Observation(params)
            calls += call
            return flow {
                try {
                    for (event in call.events) {
                        when (event) {
                            is Event.Value -> emit(event.snapshot)
                            is Event.Failed -> throw event.error
                        }
                    }
                } finally {
                    call.closed = true
                }
            }
        }
    }

    private class Call<P>(val params: P) {
        val response = CompletableDeferred<Unit>()
        var cancelled = false
    }

    private class Operation<P> {
        val calls = mutableListOf<Call<P>>()
        var ignoreCancellation = false
        var onStart: (() -> Unit)? = null

        suspend fun run(params: P) {
            val call = Call(params)
            calls += call
            onStart?.invoke()
            try {
                if (ignoreCancellation) {
                    withContext(NonCancellable) { call.response.await() }
                } else {
                    call.response.await()
                }
            } catch (cancelled: CancellationException) {
                call.cancelled = true
                throw cancelled
            }
        }
    }

    private class FakeRefresh : RefreshBrowseUseCase() {
        val operation = Operation<Params>()
        override suspend fun execute(params: Params) = operation.run(params)
    }

    private class FakePages : LoadNextBrowsePageUseCase() {
        val operation = Operation<Params>()
        override suspend fun execute(params: Params) = operation.run(params)
    }

    companion object {
        private const val USER = "user-a"
        private const val OTHER_USER = "user-b"

        private fun ready(
            ids: List<Int> = listOf(1),
            cursor: String? = "cursor-1",
            user: String = USER,
            synced: Boolean = true,
        ) = BrowseSnapshot(
            userId = user,
            clusters = ids.map { id -> Opportunity(id, "Opportunity $id", "Summary $id", "Problem $id", id, false, null) },
            nextCursor = cursor,
            hasSynced = synced,
            cacheReady = true,
        )

        private fun failure(category: OpportunityFailure) =
            OpportunityOperationException(category, IllegalStateException("Test failure"))
    }
}
