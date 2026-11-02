package com.example.problemfinder.feature.builds.impl

import androidx.lifecycle.ViewModelStore
import com.example.problemfinder.core.model.workspace.*
import com.example.problemfinder.domain.workspaces.testing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class BuildsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val fake = FakeWorkspaceWorkflows()
    @Before fun before() = Dispatchers.setMain(dispatcher)
    @After fun after() { store.clear(); dispatcher.scheduler.runCurrent(); Dispatchers.resetMain() }
    private fun model() = BuildsViewModel(fake).also { store.put("builds", it) }
    @Test fun buildListShowsReadyAnalysisAndExistingBuildsWithoutStartingGeneration() = runTest {
        fake.projects.value = WorkspaceSnapshot("user-a", listOf(
            fixtureProject(id = "watching").copy(analysisStatus = WorkspaceStatus.NOT_STARTED),
            fixtureProject(id = "analysis-ready"),
            fixtureProject(id = "existing-build").copy(analysisStatus = WorkspaceStatus.FAILED, buildStatus = WorkspaceStatus.READY)), true)
        val model = model(); model.onIntent(BuildsIntent.Entered); runCurrent()
        assertEquals(listOf("analysis-ready", "existing-build"), model.state.value.projects.map { it.id })
        assertEquals(listOf("projects.refresh"), fake.events)
    }
    @Test fun aFailedRefreshLeavesTheLastUsableBuildListVisible() = runTest {
        fake.onRefreshProjects = { throw WorkspaceException(WorkspaceFailure.NETWORK) }
        val model = model(); model.onIntent(BuildsIntent.Entered); runCurrent()
        assertEquals(1, model.state.value.projects.size)
        assertEquals(WorkspaceFailure.NETWORK, model.state.value.failure)
        assertFalse(model.state.value.isRefreshing)
    }
}
