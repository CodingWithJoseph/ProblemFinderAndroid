package com.example.problemfinder.feature.builds.impl

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.problemfinder.core.designsystem.theme.ImmensityTheme
import com.example.problemfinder.core.model.workspace.TaskProgress
import com.example.problemfinder.domain.workspaces.testing.FakeWorkspaceWorkflows
import com.example.problemfinder.feature.builds.api.BuildsDestination
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BuildWorkspaceScreenTest {
    @get:Rule val compose = createComposeRule()
    private val store = ViewModelStore()
    private val fake = FakeWorkspaceWorkflows()
    @After fun after() { compose.runOnIdle { store.clear() } }
    private fun show(panel: BuildPanel, item: String) {
        val model = BuildWorkspaceViewModel(SavedStateHandle(mapOf(BuildsDestination.PROJECT_ID to "project-1")), fake)
        store.put("build", model)
        compose.setContent {
            val state by model.state.collectAsState()
            LaunchedEffect(Unit) { model.onIntent(BuildIntent.Entered(item, panel)) }
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                ImmensityTheme(darkTheme = false) { BuildWorkspaceScreen(state, model::onIntent, panel) }
            }
        }
        compose.waitForIdle()
    }
    @Test fun doneRequiresVisibleVerificationFormAndActualUserInputAtDoubleTextSize() {
        show(BuildPanel.TASK, "task-1")
        compose.onNodeWithText("Done").performScrollTo().performClick()
        compose.onNodeWithText("Update task").performScrollTo().performClick()
        compose.runOnIdle { assertTrue(fake.taskUpdates.isEmpty()) }
        compose.onNodeWithText("Verification summary").performScrollTo().performTextInput("The task tests passed")
        compose.onNodeWithText("Successful checks or commands").performScrollTo().performTextInput("gradlew test")
        compose.onNodeWithText("Update task").performScrollTo().performClick()
        compose.waitUntil(5_000) { fake.taskUpdates.isNotEmpty() }
        compose.runOnIdle { assertEquals(TaskProgress.DONE, fake.taskUpdates.single().update.status) }
    }
    @Test fun editedDocumentCanBeSavedFromTheScrolledEditorAtDoubleTextSize() {
        show(BuildPanel.DOCUMENT, "requirements")
        compose.onNode(hasSetTextAction()).performScrollTo().performTextReplacement("A reviewed document")
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Save document"))
        compose.onNodeWithText("Save document").performClick()
        compose.waitUntil(5_000) { fake.documentSaves.isNotEmpty() }
        compose.runOnIdle { assertEquals("A reviewed document", fake.documentSaves.single().content) }
    }
    @Test fun backFromDirtyEditorOffersKeepEditingAndDiscardAtDoubleTextSize() {
        show(BuildPanel.DOCUMENT, "requirements")
        compose.onNode(hasSetTextAction()).performScrollTo().performTextReplacement("A local edit")
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Keep editing").assertIsDisplayed()
        compose.onNodeWithText("Discard").assertIsDisplayed()
        compose.onNodeWithText("Keep editing").performClick()
        compose.onNode(hasSetTextAction()).assertExists()
    }
}
