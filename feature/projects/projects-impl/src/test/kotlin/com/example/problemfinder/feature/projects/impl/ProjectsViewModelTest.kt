package com.example.problemfinder.feature.projects.impl

import androidx.lifecycle.ViewModelStore
import com.example.problemfinder.core.model.workspace.*
import com.example.problemfinder.domain.workspaces.testing.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val fake = FakeWorkspaceWorkflows()
    @Before fun before() = Dispatchers.setMain(dispatcher)
    @After fun after() { store.clear(); dispatcher.scheduler.runCurrent(); Dispatchers.resetMain() }
    private fun model() = ProjectsViewModel(fake).also { store.put("projects", it) }
    @Test fun stageSelectionUsesAnalysisLifecycleRatherThanReorderingServerResults() = runTest {
        fake.projects.value = WorkspaceSnapshot("user-a", listOf(
            fixtureProject(id = "watching").copy(analysisStatus = WorkspaceStatus.NOT_STARTED),
            fixtureProject(id = "working").copy(analysisStatus = WorkspaceStatus.GENERATING),
            fixtureProject(id = "ready"), fixtureProject(id = "stale").copy(analysisStatus = WorkspaceStatus.STALE)), true)
        val model = model(); model.onIntent(ProjectsIntent.Entered); runCurrent()
        assertEquals(listOf("watching"), model.state.value.projects.map { it.id })
        model.onIntent(ProjectsIntent.StageSelected(AnalysisStage.IN_REVIEW))
        assertEquals(listOf("working"), model.state.value.projects.map { it.id })
        model.onIntent(ProjectsIntent.StageSelected(AnalysisStage.READY))
        assertEquals(listOf("ready", "stale"), model.state.value.projects.map { it.id })
    }
    @Test fun aRoomUpdateDoesNotEndAnUnfinishedRefreshOrStartAnotherOne() = runTest {
        val complete = CompletableDeferred<Unit>()
        fake.onRefreshProjects = { complete.await() }
        val model = model(); model.onIntent(ProjectsIntent.Entered); runCurrent()
        fake.projects.value = WorkspaceSnapshot("user-a", listOf(fixtureProject(id = "another")), true); runCurrent()
        assertTrue(model.state.value.isRefreshing)
        model.onIntent(ProjectsIntent.Reload); runCurrent()
        assertEquals(1, fake.events.count { it == "projects.refresh" })
        complete.complete(Unit); runCurrent()
        assertFalse(model.state.value.isRefreshing)
    }
    @Test fun accountSwitchDropsPreviousProjectCards() = runTest {
        val model = model(); model.onIntent(ProjectsIntent.Entered); runCurrent()
        model.onIntent(ProjectsIntent.StageSelected(AnalysisStage.READY))
        assertEquals(1, model.state.value.projects.size)
        fake.projects.value = WorkspaceSnapshot("user-b", emptyList(), true); runCurrent()
        assertEquals("user-b", model.state.value.userId)
        assertTrue(model.state.value.projects.isEmpty())
    }
}
