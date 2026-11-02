package com.example.problemfinder.feature.projects.impl

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.problemfinder.core.designsystem.theme.ImmensityTheme
import com.example.problemfinder.domain.workspaces.testing.FakeWorkspaceWorkflows
import com.example.problemfinder.feature.projects.api.ProjectsDestination
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AnalysisWorkspaceScreenTest {
    @get:Rule val compose = createComposeRule()
    private val store = ViewModelStore()
    private val fake = FakeWorkspaceWorkflows()
    @After fun after() { compose.runOnIdle { store.clear() } }
    @Test fun assumptionHasAnAccessibleLabeledToggleAndSendsTheSelectedIdentity() {
        val model = AnalysisWorkspaceViewModel(SavedStateHandle(mapOf(ProjectsDestination.PROJECT_ID to "project-1")), fake)
        store.put("analysis", model)
        compose.setContent {
            val state by model.state.collectAsState()
            LaunchedEffect(Unit) { model.onIntent(AnalysisIntent.Entered(panel = AnalysisPanel.ASSUMPTIONS)) }
            ImmensityTheme(darkTheme = true) { AnalysisWorkspaceScreen(state, model::onIntent, AnalysisPanel.ASSUMPTIONS) }
        }
        compose.onNodeWithText("Does it recur?").performScrollTo().assertHasClickAction().performClick()
        compose.waitUntil(5_000) { fake.events.contains("assumption:true") }
    }
    @Test fun questionIsSentOnlyAfterTheUserSubmitsTheConversationForm() {
        val model = AnalysisWorkspaceViewModel(SavedStateHandle(mapOf(ProjectsDestination.PROJECT_ID to "project-1")), fake)
        store.put("analysis", model)
        compose.setContent {
            val state by model.state.collectAsState()
            LaunchedEffect(Unit) { model.onIntent(AnalysisIntent.Entered(panel = AnalysisPanel.CONVERSATION)) }
            ImmensityTheme { AnalysisWorkspaceScreen(state, model::onIntent, AnalysisPanel.CONVERSATION) }
        }
        compose.onNode(hasSetTextAction()).performScrollTo().performTextInput("What is actually supported?")
        compose.runOnIdle { assertFalse(fake.events.contains("conversation.send")) }
        compose.onNodeWithText("Send question").performScrollTo().performClick()
        compose.waitUntil(5_000) { fake.events.contains("conversation.send") }
    }
}
