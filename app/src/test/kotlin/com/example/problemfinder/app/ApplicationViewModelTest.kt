package com.example.problemfinder.app

import androidx.lifecycle.ViewModelStore
import com.example.problemfinder.core.mvi.RequestState
import com.example.problemfinder.data.authentication.api.AuthSession
import com.example.problemfinder.data.authentication.api.AuthSessionProvider
import com.example.problemfinder.domain.authentication.api.SignOutUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.coroutines.cancellation.CancellationException

@OptIn(ExperimentalCoroutinesApi::class)
class ApplicationViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val sessions = FakeSessions()
    private val signOut = FakeSignOut()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() {
        signOut.response.complete(Unit)
        store.clear()
        dispatcher.scheduler.runCurrent()
        Dispatchers.resetMain()
    }

    @Test
    fun identityStartsLoadingUntilTheFirstReadCompletes() = runTest {
        val model = createModel()
        assertEquals(RequestState.Loading, model.state.value.accountRequest)
        assertEquals(1, sessions.reads.size)
        assertEquals(0, sessions.credentialReads)
        assertEquals(0, signOut.calls)
    }

    @Test
    fun persistedIdentityIsAvailableWithoutRequestingCredentials() = runTest {
        val model = createModel()
        publish("user-a")
        assertEquals(RequestState.Success("user-a"), model.state.value.accountRequest)
        assertEquals(0, sessions.credentialReads)
    }

    @Test
    fun completedNullReadRepresentsNoAccountRatherThanAnError() = runTest {
        val model = createModel()
        publish(null)
        assertEquals(RequestState.Success<String?>(null), model.state.value.accountRequest)
    }

    @Test
    fun readFailureCanBeRetriedWithoutReportingSignOutFailure() = runTest {
        val model = createModel()
        val effects = mutableListOf<ApplicationEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            model.effects.collect { effects += it }
        }
        sessions.reads.single().events.trySend(Event.Error)
        runCurrent()
        assertEquals(RequestState.Error(Unit), model.state.value.accountRequest)
        assertTrue(effects.isEmpty())

        model.onIntent(ApplicationIntent.RetryAccountRead)
        runCurrent()
        assertEquals(RequestState.Loading, model.state.value.accountRequest)
        assertEquals(2, sessions.reads.size)
        assertTrue(sessions.reads.first().closed)
        publish("user-a")
        assertEquals(RequestState.Success("user-a"), model.state.value.accountRequest)
        assertTrue(effects.isEmpty())
    }

    @Test
    fun accountChangesAndSignOutAreObservedFromTheProvider() = runTest {
        val model = createModel()
        publish("user-a")
        publish("user-b")
        assertEquals(RequestState.Success("user-b"), model.state.value.accountRequest)
        publish(null)
        assertEquals(RequestState.Success<String?>(null), model.state.value.accountRequest)
        assertEquals(1, sessions.reads.size)
    }

    @Test
    fun repeatedSignOutIntentsAreGatedAndIdentityWaitsForTheProvider() = runTest {
        val model = createModel()
        publish("user-a")
        repeat(3) { model.onIntent(ApplicationIntent.SignOutClicked) }
        runCurrent()
        assertEquals(1, signOut.calls)
        assertTrue(model.state.value.isSigningOut)
        signOut.response.complete(Unit)
        runCurrent()
        assertFalse(model.state.value.isSigningOut)
        assertEquals(RequestState.Success("user-a"), model.state.value.accountRequest)
        publish(null)
        assertEquals(RequestState.Success<String?>(null), model.state.value.accountRequest)
    }

    @Test
    fun signOutFailurePreservesIdentityAndEmitsRetryEffect() = runTest {
        val model = createModel()
        publish("user-a")
        val effects = mutableListOf<ApplicationEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            model.effects.collect { effects += it }
        }
        model.onIntent(ApplicationIntent.SignOutClicked)
        runCurrent()
        signOut.response.completeExceptionally(IllegalStateException("Offline"))
        runCurrent()
        assertFalse(model.state.value.isSigningOut)
        assertEquals(RequestState.Success("user-a"), model.state.value.accountRequest)
        assertEquals(listOf(ApplicationEffect.SignOutFailed), effects)
    }

    @Test
    fun clearingViewModelCancelsIdentityObservationAndSignOut() = runTest {
        val model = createModel()
        publish("user-a")
        model.onIntent(ApplicationIntent.SignOutClicked)
        runCurrent()
        store.clear()
        runCurrent()
        assertTrue(sessions.reads.single().closed)
        assertTrue(signOut.cancelled)
    }

    private fun TestScope.createModel(): ApplicationViewModel {
        val model = ApplicationViewModel(signOut, sessions)
        store.put("application", model)
        runCurrent()
        return model
    }

    private fun TestScope.publish(userId: String?) {
        check(sessions.reads.last().events.trySend(Event.Identity(userId)).isSuccess)
        runCurrent()
    }

    private sealed interface Event {
        data class Identity(val userId: String?) : Event
        data object Error : Event
    }

    private class Read {
        val events = Channel<Event>(Channel.UNLIMITED)
        var closed = false
    }

    private class FakeSessions : AuthSessionProvider {
        val reads = mutableListOf<Read>()
        var credentialReads = 0

        override fun observeUserId(): Flow<String?> {
            val read = Read()
            reads += read
            return flow {
                try {
                    for (event in read.events) {
                        when (event) {
                            is Event.Identity -> emit(event.userId)
                            Event.Error -> throw IllegalStateException("Persisted identity unreadable")
                        }
                    }
                } finally {
                    read.closed = true
                }
            }
        }

        override suspend fun currentSession(): AuthSession? {
            credentialReads += 1
            return null
        }
    }

    private class FakeSignOut : SignOutUseCase() {
        val response = CompletableDeferred<Unit>()
        var calls = 0
        var cancelled = false

        override suspend fun execute(params: Unit) {
            calls += 1
            try {
                response.await()
            } catch (error: CancellationException) {
                cancelled = true
                throw error
            }
        }
    }
}
