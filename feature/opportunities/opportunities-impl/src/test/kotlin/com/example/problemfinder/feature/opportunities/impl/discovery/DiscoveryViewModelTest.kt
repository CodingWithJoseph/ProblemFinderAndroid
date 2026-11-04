package com.example.problemfinder.feature.opportunities.impl.discovery

import com.example.problemfinder.domain.opportunities.api.actions.SetOpportunityWatchedUseCase
import androidx.lifecycle.ViewModelStore
import com.example.problemfinder.core.model.opportunity.OpportunityFailure
import com.example.problemfinder.core.model.opportunity.OpportunityOperationException
import com.example.problemfinder.core.model.opportunity.OpportunityCategory
import com.example.problemfinder.core.model.opportunity.Opportunity
import com.example.problemfinder.core.model.discovery.DiscoveryCollection
import com.example.problemfinder.core.model.discovery.DiscoveryDashboard
import com.example.problemfinder.core.model.discovery.DiscoverySnapshot
import com.example.problemfinder.core.mvi.RequestState
import com.example.problemfinder.domain.opportunities.api.discovery.ObserveDiscoveryUseCase
import com.example.problemfinder.domain.opportunities.api.discovery.RefreshDiscoveryUseCase
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
class DiscoveryViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val observe = FakeObserve()
    private val actions = FakeActions()
    private val refresh = FakeRefresh()

    @Before
    fun setUp() { Dispatchers.setMain(dispatcher) }

    @After
    fun tearDown() {
        // Release deliberately cancellation-resistant requests before clearing the VM.
        refresh.calls.forEach { it.response.complete(Unit) }
        store.clear()
        dispatcher.scheduler.runCurrent()
        Dispatchers.resetMain()
    }

    @Test
    fun watchActionDisablesOnlyItsOpportunityAndIgnoresDuplicateTaps() = runTest {
        val model = createModel()
        publish(ready(listOf(42, 17)))
        finishRefresh()
        model.onIntent(DiscoveryIntent.WatchCluster(42))
        model.onIntent(DiscoveryIntent.WatchCluster(42))
        model.onIntent(DiscoveryIntent.WatchCluster(17))
        runCurrent()
        assertEquals(2, actions.calls.size)
        assertEquals(SetOpportunityWatchedUseCase.Params(USER, 42, true), actions.calls.first().params)
        assertEquals(setOf(42, 17), model.state.value.pendingClusterIds)
        actions.calls.first().response.complete(Unit)
        runCurrent()
        assertEquals(setOf(17), model.state.value.pendingClusterIds)
        actions.calls.last().response.complete(Unit)
        runCurrent()
        assertTrue(model.state.value.pendingClusterIds.isEmpty())
    }

    @Test
    fun failedWatchPreservesContentAndShowsAnActionError() = runTest {
        val model = createModel()
        publish(ready(listOf(42)))
        finishRefresh()
        model.onIntent(DiscoveryIntent.WatchCluster(42))
        runCurrent()
        actions.calls.single().response.completeExceptionally(OpportunityOperationException(OpportunityFailure.NETWORK, java.io.IOException("Offline")))
        runCurrent()
        assertTrue(model.state.value.pendingClusterIds.isEmpty())
        assertFalse(model.state.value.collections.single().clusters.single().isWatched)
        assertEquals(RequestState.Success(Unit), model.state.value.refreshRequestState)
        assertEquals(DiscoveryEffect.ShowError(com.example.problemfinder.feature.opportunities.impl.util.OpportunityUIError(OpportunityFailure.NETWORK)), model.effects.first())
    }

    @Test
    fun accountSwitchClearsPendingActionsAndDoesNotReportTheirStaleFailure() = runTest {
        val model = createModel()
        publish(ready(listOf(42)))
        finishRefresh()
        model.onIntent(DiscoveryIntent.WatchCluster(42))
        runCurrent()
        publish(ready(listOf(17), userId = OTHER_USER))
        assertTrue(model.state.value.pendingClusterIds.isEmpty())
        assertEquals(OTHER_USER, model.state.value.userId)
        assertEquals(listOf(17), model.state.value.collections.single().clusters.map { it.id })
        finishRefresh()
    }

    @Test
    fun selectedOpportunityAndCollectionEmitNavigationEffects() = runTest {
        val model = createModel()
        model.onIntent(DiscoveryIntent.OpenCluster(42))
        runCurrent()
        assertEquals(DiscoveryEffect.NavigateToCluster(42), model.effects.first())
        model.onIntent(DiscoveryIntent.SeeAll(OpportunityCategory.DATA_AND_APIS))
        runCurrent()
        assertEquals(DiscoveryEffect.NavigateToBrowse(OpportunityCategory.DATA_AND_APIS), model.effects.first())
    }

    @Test
    fun entryWaitsForCacheAndPublishesContentBeforeRefreshingOnce() = runTest {
        val model = createModel()
        repeat(3) { model.onIntent(DiscoveryIntent.Entered) }
        runCurrent()
        assertEquals(1, observe.calls.size)
        assertTrue(refresh.calls.isEmpty())
        publish(loading(USER))
        assertFalse(model.state.value.cacheReady)
        assertTrue(refresh.calls.isEmpty())

        var stateAtRequest: DiscoveryState? = null
        refresh.onStart = { stateAtRequest = model.state.value }
        publish(ready(listOf(42, 17)))
        assertEquals(42, stateAtRequest?.featuredCluster?.id)
        assertEquals(listOf(42, 17), stateAtRequest?.collections?.single()?.clusters?.map { it.id })
        assertTrue(model.state.value.hasSynced)
        assertEquals(RequestState.Loading, model.state.value.refreshRequestState)
        assertEquals(1, refresh.calls.size)

        publish(ready(listOf(8)))
        assertEquals(1, refresh.calls.size)
        assertFalse(refresh.calls.single().cancelled)
        finishRefresh()
        publish(ready(listOf(9)))
        assertEquals(1, refresh.calls.size)
        assertEquals(9, model.state.value.featuredCluster?.id)
    }

    @Test
    fun failedRefreshRetainsCacheAndRetryDoesNotRestartObservation() = runTest {
        val model = createModel()
        publish(ready(listOf(42)))
        refresh.calls.single().response.completeExceptionally(failure(OpportunityFailure.NETWORK))
        runCurrent()
        assertEquals(42, model.state.value.featuredCluster?.id)
        assertTrue(model.state.value.hasSynced)
        assertEquals(RequestState.Success(Unit), model.state.value.observationRequest)
        assertEquals(OpportunityFailure.NETWORK, model.state.value.contentError?.failure)
        assertTrue(model.state.value.canRefresh)

        repeat(3) { model.onIntent(DiscoveryIntent.Reload) }
        runCurrent()
        assertEquals(2, refresh.calls.size)
        assertEquals(1, observe.calls.size)
        assertNull(model.state.value.contentError)
        assertFalse(model.state.value.canRefresh)
        finishRefresh()
        assertEquals(RequestState.Success(Unit), model.state.value.refreshRequestState)
        assertEquals(42, model.state.value.featuredCluster?.id)
    }

    @Test
    fun failedFirstRefreshDoesNotTurnMissingCacheIntoASuccessfulEmptyResult() = runTest {
        val model = createModel()
        publish(ready(emptyList(), synced = false))
        refresh.calls.single().response.completeExceptionally(failure(OpportunityFailure.SERVER))
        runCurrent()
        assertTrue(model.state.value.cacheReady)
        assertFalse(model.state.value.hasSynced)
        assertFalse(model.state.value.hasContent)
        assertEquals(OpportunityFailure.SERVER, model.state.value.contentError?.failure)
        assertTrue(model.state.value.canRefresh)
    }

    @Test
    fun successfulEmptyCacheIsSyncedAndDoesNotTriggerAnotherRefresh() = runTest {
        val model = createModel()
        publish(ready(emptyList(), synced = false))
        finishRefresh()
        // The cache supplies content and sync metadata, not the refresh return value.
        assertFalse(model.state.value.hasSynced)
        publish(ready(emptyList()))
        assertTrue(model.state.value.hasSynced)
        assertFalse(model.state.value.hasContent)
        assertNull(model.state.value.contentError)
        assertEquals(1, refresh.calls.size)
        assertTrue(model.state.value.canRefresh)
    }

    @Test
    fun observationFailureRetainsCardsCancelsRefreshAndRetryReadsCacheFirst() = runTest {
        val model = createModel()
        publish(ready(listOf(42)))
        observe.calls.last().events.send(Event.Failed(IllegalStateException("Cache unavailable")))
        runCurrent()
        assertTrue(refresh.calls.single().cancelled)
        assertEquals(42, model.state.value.featuredCluster?.id)
        assertEquals(OpportunityFailure.UNKNOWN, model.state.value.contentError?.failure)
        assertEquals(RequestState.Idle, model.state.value.refreshRequestState)
        assertTrue(model.state.value.canRefresh)

        model.onIntent(DiscoveryIntent.Reload)
        runCurrent()
        assertEquals(2, observe.calls.size)
        assertEquals(1, refresh.calls.size)
        publish(loading(USER))
        assertEquals(42, model.state.value.featuredCluster?.id)
        assertFalse(model.state.value.cacheReady)
        publish(ready(listOf(17)))
        assertEquals(17, model.state.value.featuredCluster?.id)
        assertEquals(2, refresh.calls.size)
        assertNull(model.state.value.contentError)
    }

    @Test
    fun observationFailureBeforeAccountIsKnownCanBeRetried() = runTest {
        val model = createModel()
        observe.calls.last().events.send(Event.Failed(IllegalStateException("Identity unavailable")))
        runCurrent()
        assertNull(model.state.value.userId)
        assertTrue(model.state.value.canRefresh)
        model.onIntent(DiscoveryIntent.Reload)
        runCurrent()
        assertEquals(2, observe.calls.size)
        assertTrue(refresh.calls.isEmpty())
        publish(ready())
        assertEquals(1, refresh.calls.size)
        assertNull(model.state.value.contentError)
    }

    @Test
    fun signOutClearsContentAndTheNextAccountRefreshesItsOwnCache() = runTest {
        val model = createModel()
        publish(ready(listOf(42)))
        publish(DiscoverySnapshot(null, EMPTY_DASHBOARD))
        assertTrue(refresh.calls.single().cancelled)
        assertNull(model.state.value.userId)
        assertNull(model.state.value.featuredCluster)
        assertTrue(model.state.value.collections.isEmpty())
        assertFalse(model.state.value.canRefresh)
        assertEquals(RequestState.Idle, model.state.value.refreshRequestState)
        publish(loading(OTHER_USER))
        assertEquals(1, refresh.calls.size)
        publish(ready(listOf(17), userId = OTHER_USER))
        assertEquals(OTHER_USER, model.state.value.userId)
        assertEquals(17, model.state.value.featuredCluster?.id)
        assertEquals(2, refresh.calls.size)
    }

    @Test
    fun lateSuccessFromOldAccountCannotCompleteTheNewAccountsRefresh() = runTest {
        assertLateResultIgnored(null)
    }

    @Test
    fun lateFailureFromOldAccountCannotSetTheNewAccountsError() = runTest {
        assertLateResultIgnored(failure(OpportunityFailure.SERVER))
    }

    @Test
    fun cancellationDoesNotBecomeAnErrorAndManualRetryStillWorks() = runTest {
        val model = createModel()
        publish(ready())
        refresh.calls.single().response.completeExceptionally(CancellationException("Cancelled"))
        runCurrent()
        assertEquals(RequestState.Idle, model.state.value.refreshRequestState)
        assertNull(model.state.value.contentError)
        model.onIntent(DiscoveryIntent.Reload)
        runCurrent()
        assertEquals(2, refresh.calls.size)
        finishRefresh()
        assertEquals(RequestState.Success(Unit), model.state.value.refreshRequestState)
    }

    @Test
    fun emptyCollectionRecordsDoNotCountAsOpportunityContent() = runTest {
        val model = createModel()
        publish(
            DiscoverySnapshot(
                userId = USER,
                dashboard = DiscoveryDashboard(
                    featuredCluster = null,
                    collections = listOf(DiscoveryCollection(OpportunityCategory.DATA_AND_APIS, emptyList())),
                ),
                hasSynced = true,
            )
        )
        finishRefresh()
        assertTrue(model.state.value.collections.isNotEmpty())
        assertFalse(model.state.value.hasContent)
        assertTrue(model.state.value.hasSynced)
    }

    @Test
    fun clearingViewModelCancelsObservationAndRefresh() = runTest {
        createModel()
        publish(ready())
        store.clear()
        runCurrent()
        assertTrue(observe.calls.single().closed)
        assertTrue(refresh.calls.single().cancelled)
    }

    private suspend fun TestScope.assertLateResultIgnored(error: Exception?) {
        refresh.ignoreCancellation = true
        val model = createModel()
        publish(ready(listOf(42)))
        val oldRequest = refresh.calls.single()
        publish(loading(OTHER_USER))
        assertNull(model.state.value.featuredCluster)
        publish(ready(listOf(17), userId = OTHER_USER))
        if (error == null) oldRequest.response.complete(Unit)
        else oldRequest.response.completeExceptionally(error)
        runCurrent()
        assertEquals(OTHER_USER, model.state.value.userId)
        assertEquals(17, model.state.value.featuredCluster?.id)
        assertEquals(RequestState.Loading, model.state.value.refreshRequestState)
        assertNull(model.state.value.contentError)
        assertEquals(1, observe.calls.size)
        finishRefresh()
        assertEquals(RequestState.Success(Unit), model.state.value.refreshRequestState)
    }

    private fun TestScope.createModel(): DiscoveryViewModel {
        val model = DiscoveryViewModel(observe, refresh, actions)
        store.put("discovery", model)
        model.onIntent(DiscoveryIntent.Entered)
        runCurrent()
        return model
    }

    private suspend fun TestScope.publish(snapshot: DiscoverySnapshot) {
        observe.calls.last().events.send(Event.Value(snapshot))
        runCurrent()
    }

    private fun TestScope.finishRefresh() {
        refresh.calls.last().response.complete(Unit)
        runCurrent()
    }

    private sealed interface Event {
        data class Value(val snapshot: DiscoverySnapshot) : Event
        data class Failed(val error: Exception) : Event
    }

    private class Observation {
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

    private class FakeObserve : ObserveDiscoveryUseCase() {
        val calls = mutableListOf<Observation>()
        override suspend fun execute(params: Unit): Flow<DiscoverySnapshot> {
            val call = Observation()
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

    private class RefreshCall {
        val response = CompletableDeferred<Unit>()
        var cancelled = false
    }

    private class FakeRefresh : RefreshDiscoveryUseCase() {
        val calls = mutableListOf<RefreshCall>()
        var ignoreCancellation = false
        var onStart: (() -> Unit)? = null
        override suspend fun execute(params: Unit) {
            val call = RefreshCall()
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

    companion object {
        private const val USER = "user-a"
        private const val OTHER_USER = "user-b"
        private val EMPTY_DASHBOARD = DiscoveryDashboard(null, emptyList())
        private fun loading(userId: String) = DiscoverySnapshot(
            userId = userId,
            dashboard = EMPTY_DASHBOARD,
            cacheReady = false,
        )

        private fun ready(
            ids: List<Int> = listOf(42),
            userId: String = USER,
            synced: Boolean = true,
        ): DiscoverySnapshot {
            val clusters = ids.map { id ->
                Opportunity(
                    id = id,
                    name = "Opportunity $id",
                    summary = "Summary $id",
                    problemStatement = "Problem $id",
                    postCount = id,
                    isWatched = false,
                    date = null,
                    category = OpportunityCategory.BUILD_AND_TOOLING,
                )
            }
            return DiscoverySnapshot(
                userId = userId,
                dashboard = DiscoveryDashboard(
                    featuredCluster = clusters.firstOrNull(),
                    collections = listOf(DiscoveryCollection(OpportunityCategory.BUILD_AND_TOOLING, clusters)),
                ),
                hasSynced = synced,
            )
        }

        private fun failure(failure: OpportunityFailure) =
            OpportunityOperationException(failure, IllegalStateException("Test failure"))
    }
}
