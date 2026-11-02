package com.example.problemfinder.feature.projects.impl

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.example.problemfinder.core.model.workspace.*
import com.example.problemfinder.domain.workspaces.testing.*
import com.example.problemfinder.feature.projects.api.ProjectsDestination
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AnalysisWorkspaceViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val fake = FakeWorkspaceWorkflows()
    @Before fun before() = Dispatchers.setMain(dispatcher)
    @After fun after() { store.clear(); dispatcher.scheduler.runCurrent(); Dispatchers.resetMain() }
    private fun model(): AnalysisWorkspaceViewModel = AnalysisWorkspaceViewModel(
        SavedStateHandle(mapOf(ProjectsDestination.PROJECT_ID to "project-1")), fake).also { store.put("model", it) }

    @Test fun opensCachedAnalysisAndRefreshesOnceWithoutRegeneratingOnCacheUpdates() = runTest {
        val model = model(); model.onIntent(AnalysisIntent.Entered()); runCurrent()
        assertEquals("A useful project", model.state.value.workspace?.title)
        assertEquals(1, fake.events.count { it == "analysis.refresh" })
        fake.analysis.value = WorkspaceSnapshot("user-a", fixtureAnalysis().copy(analysis = fixtureAnalysis().analysis?.copy(version = 2)), true)
        runCurrent()
        assertEquals(1, fake.events.count { it == "analysis.refresh" })
        assertFalse(fake.events.contains("analysis.generate"))
    }
    @Test fun failedRefreshKeepsSavedAnalysisAndCanBeRetried() = runTest {
        fake.onRefreshAnalysis = { throw WorkspaceException(WorkspaceFailure.NETWORK) }
        val model = model(); model.onIntent(AnalysisIntent.Entered()); runCurrent()
        assertNotNull(model.state.value.workspace)
        assertEquals(WorkspaceFailure.NETWORK, model.state.value.failure)
        fake.onRefreshAnalysis = {}
        model.onIntent(AnalysisIntent.Reload); runCurrent()
        assertNull(model.state.value.failure)
        assertFalse(model.state.value.isRefreshing)
    }
    @Test fun duplicateGenerationIsBlockedAndOldAccountCompletionCannotAlterNewAccount() = runTest {
        val gate = CompletableDeferred<Unit>()
        fake.onGenerateAnalysis = { withContext(NonCancellable) { gate.await() } }
        val model = model(); model.onIntent(AnalysisIntent.Entered()); runCurrent()
        model.onIntent(AnalysisIntent.Generate); model.onIntent(AnalysisIntent.Generate); runCurrent()
        assertEquals(1, fake.events.count { it == "analysis.generate" })
        fake.analysis.value = WorkspaceSnapshot("user-b", fixtureAnalysis("user-b"), true); runCurrent()
        assertEquals("user-b", model.state.value.userId)
        assertFalse(model.state.value.isMutating)
        gate.complete(Unit); runCurrent()
        assertEquals("user-b", model.state.value.userId)
        assertNull(model.state.value.failure)
    }
    @Test fun generationPollingStopsWhenTheWorkspaceIsNotVisible() = runTest {
        fake.analysis.value = WorkspaceSnapshot("user-a", fixtureAnalysis(status = WorkspaceStatus.GENERATING), true)
        val model = model(); model.onIntent(AnalysisIntent.Entered())
        model.onIntent(AnalysisIntent.VisibilityChanged("screen", true)); runCurrent()
        advanceTimeBy(2_001); runCurrent()
        assertEquals(2, fake.events.count { it == "analysis.refresh" })
        model.onIntent(AnalysisIntent.VisibilityChanged("screen", false))
        advanceTimeBy(10_000); runCurrent()
        assertEquals(2, fake.events.count { it == "analysis.refresh" })
    }
    @Test fun selectedProblemAndAssumptionActionsUseTheRealDocumentIdentity() = runTest {
        val model = model(); model.onIntent(AnalysisIntent.Entered("problem-1", AnalysisPanel.PROBLEM)); runCurrent()
        assertEquals("problem-1", model.state.value.selectedProblem?.id)
        model.onIntent(AnalysisIntent.PinProblem("missing")); runCurrent()
        assertFalse(fake.events.contains("problem.pin:true"))
        model.onIntent(AnalysisIntent.PinProblem("problem-1")); runCurrent()
        model.onIntent(AnalysisIntent.ResolveAssumption("assumption-1", true)); runCurrent()
        assertTrue(fake.events.contains("problem.pin:true"))
        assertTrue(fake.events.contains("assumption:true"))
    }
    @Test fun failedMessageKeepsDraftAndReusesCreatedConversationOnRetry() = runTest {
        fake.onSend = { throw WorkspaceException(WorkspaceFailure.NETWORK) }
        val model = model(); model.onIntent(AnalysisIntent.Entered(panel = AnalysisPanel.CONVERSATION)); runCurrent()
        model.onIntent(AnalysisIntent.MessageChanged("What does the evidence show?"))
        model.onIntent(AnalysisIntent.SendMessage); runCurrent()
        assertEquals("What does the evidence show?", model.state.value.draftMessage)
        assertEquals("conversation-1", model.state.value.conversationId)
        assertFalse(model.state.value.isSending)
        fake.onSend = {}
        model.onIntent(AnalysisIntent.SendMessage); runCurrent()
        assertEquals(1, fake.events.count { it == "conversation.create" })
        assertEquals("", model.state.value.draftMessage)
    }
    @Test fun restartingFailedObservationKeepsSameAccountContentWhileTheCacheIsReadAgain() = runTest {
        val gate = CompletableDeferred<Unit>()
        var reads = 0
        val boundary = object : FakeWorkspaceWorkflows() {
            override fun observeAnalysis(projectId: String) = kotlinx.coroutines.flow.flow {
                if (reads++ == 0) {
                    emit(WorkspaceSnapshot("user-a", fixtureAnalysis(), true))
                    throw WorkspaceException(WorkspaceFailure.UNKNOWN)
                }
                emit(WorkspaceSnapshot<AnalysisWorkspaceData>("user-a", cacheReady = false))
                gate.await()
                emit(WorkspaceSnapshot("user-a", fixtureAnalysis(), true))
            }
        }
        val model = AnalysisWorkspaceViewModel(SavedStateHandle(mapOf(ProjectsDestination.PROJECT_ID to "project-1")), boundary)
        store.put("model", model)
        model.onIntent(AnalysisIntent.Entered()); runCurrent()
        assertNotNull(model.state.value.workspace)
        model.onIntent(AnalysisIntent.Reload); runCurrent()
        assertNotNull(model.state.value.workspace)
        assertFalse(model.state.value.cacheReady)
        gate.complete(Unit); runCurrent()
        assertTrue(model.state.value.cacheReady)
    }
}
