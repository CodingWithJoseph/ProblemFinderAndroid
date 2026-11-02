package com.example.problemfinder.feature.builds.impl

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.example.problemfinder.core.model.workspace.*
import com.example.problemfinder.domain.workspaces.testing.*
import com.example.problemfinder.feature.builds.api.BuildsDestination
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BuildWorkspaceViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val fake = FakeWorkspaceWorkflows()
    @Before fun before() = Dispatchers.setMain(dispatcher)
    @After fun after() { store.clear(); dispatcher.scheduler.runCurrent(); Dispatchers.resetMain() }
    private fun model(): BuildWorkspaceViewModel = BuildWorkspaceViewModel(
        SavedStateHandle(mapOf(BuildsDestination.PROJECT_ID to "project-1")), fake).also { store.put("model", it) }

    @Test fun openingBuildDoesNotGenerateOrApproveIt() = runTest {
        val model = model(); model.onIntent(BuildIntent.Entered()); runCurrent()
        assertNotNull(model.state.value.workspace)
        assertEquals(listOf("build.refresh"), fake.events)
    }
    @Test fun taskDoneRequiresUserVerificationAndUsesTheDisplayedVersion() = runTest {
        val model = model(); model.onIntent(BuildIntent.Entered("task-1", BuildPanel.TASK)); runCurrent()
        model.onIntent(BuildIntent.TaskStatusSelected(BuildTaskStatus.DONE)); runCurrent()
        assertTrue(fake.taskUpdates.isEmpty())
        model.onIntent(BuildIntent.ConfirmTaskStatus); runCurrent()
        assertEquals(WorkspaceFailure.REJECTED, model.state.value.failure)
        model.onIntent(BuildIntent.VerificationSummaryChanged("The focused test passed"))
        model.onIntent(BuildIntent.VerificationChecksChanged("gradlew test"))
        model.onIntent(BuildIntent.ConfirmTaskStatus); runCurrent()
        assertEquals(1, fake.taskUpdates.size)
        assertEquals(1, fake.taskUpdates.first().version)
        assertEquals("gradlew test", fake.taskUpdates.first().update.checks.first().command)
    }
    @Test fun localDraftSurvivesReloadAndConflictWithoutBeingOverwritten() = runTest {
        fake.onSaveDocument = { throw WorkspaceException(WorkspaceFailure.CONFLICT) }
        val model = model(); model.onIntent(BuildIntent.Entered("requirements", BuildPanel.DOCUMENT)); runCurrent()
        model.onIntent(BuildIntent.DocumentChanged("My local changes")); advanceTimeBy(301); runCurrent()
        assertEquals("My local changes", fake.drafts.values.single().content)
        fake.build.value = WorkspaceSnapshot("user-a", fixtureBuild(version = 2, content = "Another user's edit"), true); runCurrent()
        assertEquals("My local changes", model.state.value.documentDraft)
        assertEquals(1, model.state.value.documentBaseVersion)
        model.onIntent(BuildIntent.SaveDocument); runCurrent()
        assertEquals(1, fake.documentSaves.single().version)
        assertEquals("My local changes", model.state.value.documentDraft)
        assertTrue(model.state.value.documentDirty)
        assertFalse(model.state.value.isSaving)
        assertEquals(WorkspaceFailure.CONFLICT, model.state.value.failure)
    }
    @Test fun savedDraftRestoresItsOriginalVersionAfterRecreation() = runTest {
        fake.drafts[Triple("user-a", "project-1", "requirements")] = DocumentDraft("requirements", 1, "Recovered edit")
        fake.build.value = WorkspaceSnapshot("user-a", fixtureBuild(version = 3), true)
        val model = model(); model.onIntent(BuildIntent.Entered("requirements", BuildPanel.DOCUMENT)); runCurrent()
        assertEquals("Recovered edit", model.state.value.documentDraft)
        assertEquals(1, model.state.value.documentBaseVersion)
        assertTrue(model.state.value.documentDirty)
    }
    @Test fun typingBeforeSavedDraftReadFinishesCannotBeOverwrittenByThatRead() = runTest {
        val gate = CompletableDeferred<Unit>()
        fake.onReadDraft = { gate.await() }
        fake.drafts[Triple("user-a", "project-1", "requirements")] = DocumentDraft("requirements", 1, "Old local edit")
        val model = model(); model.onIntent(BuildIntent.Entered("requirements", BuildPanel.DOCUMENT)); runCurrent()
        model.onIntent(BuildIntent.DocumentChanged("New typing"))
        gate.complete(Unit); runCurrent()
        assertEquals("New typing", model.state.value.documentDraft)
    }
    @Test fun changingAccountClearsVisibleDraftAndDoesNotStoreItUnderTheNewOwner() = runTest {
        val model = model(); model.onIntent(BuildIntent.Entered("requirements", BuildPanel.DOCUMENT)); runCurrent()
        model.onIntent(BuildIntent.DocumentChanged("Account A edit"))
        fake.build.value = WorkspaceSnapshot("user-b", fixtureBuild("user-b", content = "Account B document"), true)
        runCurrent(); advanceTimeBy(500); runCurrent()
        assertEquals("user-b", model.state.value.userId)
        assertEquals("Account B document", model.state.value.documentDraft)
        assertTrue(fake.drafts.keys.none { it.first == "user-b" })
    }
    @Test fun successfulSaveAcknowledgesTheNewVersionAndBackAsksAboutDirtyEdits() = runTest {
        val model = model(); model.onIntent(BuildIntent.Entered("requirements", BuildPanel.DOCUMENT)); runCurrent()
        model.onIntent(BuildIntent.DocumentChanged("Edited document")); model.onIntent(BuildIntent.Back)
        assertTrue(model.state.value.confirmLeave)
        model.onIntent(BuildIntent.KeepEditing); model.onIntent(BuildIntent.SaveDocument); runCurrent()
        assertEquals(2, model.state.value.documentBaseVersion)
        assertFalse(model.state.value.documentDirty)
        assertFalse(model.state.value.isSaving)
    }
    @Test fun generationStartsFromTheExistingDeliveryTargetInsteadOfSilentlySwitchingPlatform() = runTest {
        var sent: BuildGeneration? = null
        fake.onGenerateBuild = { sent = it }
        val model = model(); model.onIntent(BuildIntent.Entered()); runCurrent()
        assertEquals(setOf(DeliveryTarget.ANDROID), model.state.value.deliveryTargets)
        model.onIntent(BuildIntent.Generate); runCurrent()
        assertEquals(setOf(DeliveryTarget.ANDROID), sent?.targets)
    }
    @Test fun aRemovedDocumentDoesNotTrapTheUserBehindTheUnsavedChangesDialog() = runTest {
        val model = model(); model.onIntent(BuildIntent.Entered("requirements", BuildPanel.DOCUMENT)); runCurrent()
        model.onIntent(BuildIntent.DocumentChanged("Keep this text"))
        val update = fixtureBuild(version = 2).let { it.copy(solution = it.solution.copy(documents = emptyList())) }
        fake.build.value = WorkspaceSnapshot("user-a", update, true); runCurrent()
        assertNull(model.state.value.selectedDocument)
        model.onIntent(BuildIntent.Back)
        assertTrue(model.state.value.confirmLeave)
        model.onIntent(BuildIntent.DiscardAndLeave); runCurrent()
        assertFalse(model.state.value.documentDirty)
        assertFalse(model.state.value.confirmLeave)
    }
}
