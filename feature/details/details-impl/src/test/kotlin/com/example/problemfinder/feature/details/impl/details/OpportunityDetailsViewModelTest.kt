package com.example.problemfinder.feature.details.impl.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.problemfinder.core.model.details.OpportunityDetailsSnapshot
import com.example.problemfinder.core.model.opportunity.Opportunity
import com.example.problemfinder.core.mvi.RequestState
import com.example.problemfinder.domain.opportunities.api.actions.SetOpportunityWatchedUseCase
import com.example.problemfinder.domain.opportunities.api.details.*
import com.example.problemfinder.feature.details.api.OpportunityDetailsDestination
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OpportunityDetailsViewModelTest {
    @Test
    fun retryRearmsRefreshAndWaitsForTheReplacementObserver() = runTest {
        val failFirstObservation = CompletableDeferred<Unit>()
        val finishRetryRead = CompletableDeferred<Unit>()
        var reads = 0
        var requests = 0
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val vm = create(
            observe = object : ObserveOpportunityDetailsUseCase() {
                override suspend fun execute(params: Params) = flow {
                    if (++reads == 1) {
                        emit(ready)
                        failFirstObservation.await()
                        error("Room observation failed")
                    }
                    emit(ready.copy(cacheReady = false))
                    finishRetryRead.await()
                    emit(ready)
                    awaitCancellation()
                }
            },
            refresh = { requests++ },
        )
        try {
            runCurrent()
            assertEquals(1, requests)
            failFirstObservation.complete(Unit)
            runCurrent()
            assertTrue(vm.state.value.observationRequest is RequestState.Error)
            assertNotNull(vm.state.value.cachedOpportunity)
            vm.onIntent(OpportunityDetailsIntent.Reload)
            runCurrent()
            assertEquals(1, requests)
            assertTrue(vm.state.value.observationRequest is RequestState.Loading)
            finishRetryRead.complete(Unit)
            runCurrent()
            assertEquals(2, requests)
            assertTrue(vm.state.value.refreshRequest is RequestState.Success)
        } finally {
            vm.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun refreshCannotInterruptAppendAndRetiresThePreviousPageError() = runTest {
        val finishAppend = CompletableDeferred<Unit>()
        var requests = 0
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val vm = create(observe = ongoingObservation(), refresh = { requests++ }, append = {
            finishAppend.await()
            error("Page request failed")
        })
        try {
            runCurrent()
            vm.onIntent(OpportunityDetailsIntent.LoadNextEvidencePage)
            runCurrent()
            vm.onIntent(OpportunityDetailsIntent.Reload)
            assertEquals(1, requests)
            assertTrue(vm.state.value.pageRequest is RequestState.Loading)
            finishAppend.complete(Unit)
            runCurrent()
            assertTrue(vm.state.value.pageRequest is RequestState.Error)
            vm.onIntent(OpportunityDetailsIntent.Reload)
            runCurrent()
            assertEquals(2, requests)
            assertEquals(RequestState.Idle, vm.state.value.pageRequest)
        } finally {
            vm.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun addUsesDesiredMembershipAndDuplicateIntentIsIgnoredUntilCompletion() = runTest {
        val finishWatch = CompletableDeferred<Unit>()
        val desired = mutableListOf<Boolean>()
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val vm = create(observe = ongoingObservation(), watch = {
            desired += it
            finishWatch.await()
        })
        try {
            runCurrent()
            repeat(2) { vm.onIntent(OpportunityDetailsIntent.WatchClicked) }
            runCurrent()
            assertEquals(listOf(true), desired)
            assertTrue(vm.state.value.isWatchPending)
            finishWatch.complete(Unit)
            runCurrent()
            assertFalse(vm.state.value.isWatchPending)
        } finally {
            vm.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }

    private val ready = OpportunityDetailsSnapshot(
        userId = "account",
        cachedOpportunity = Opportunity(42, "Name", "Summary", "Problem", 40, false, null),
        cacheReady = true, hasSynced = true, lastLoadedPage = 1, pageSize = 20, totalPosts = 40,
    )

    private fun ongoingObservation() = object : ObserveOpportunityDetailsUseCase() {
        override suspend fun execute(params: Params) = flow {
            emit(ready)
            awaitCancellation()
        }
    }

    private fun create(
        observe: ObserveOpportunityDetailsUseCase,
        refresh: suspend () -> Unit = {},
        append: suspend () -> Unit = {},
        watch: suspend (Boolean) -> Unit = {},
    ) = OpportunityDetailsViewModel(
        SavedStateHandle(mapOf(OpportunityDetailsDestination.ID_ARGUMENT to 42)),
        observe,
        object : RefreshOpportunityDetailsUseCase() { override suspend fun execute(params: Params) = refresh() },
        object : LoadNextOpportunityEvidenceUseCase() { override suspend fun execute(params: Params) = append() },
        object : SetOpportunityWatchedUseCase() { override suspend fun execute(params: Params) = watch(params.watched) },
    )
}
