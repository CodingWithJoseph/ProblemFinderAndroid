package com.example.problemfinder.data.workspaces.impl

import com.example.problemfinder.core.model.workspace.*
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

/** Fixtures were exported from the backend's Pydantic contracts, including their default fields. */
class WorkspaceContractTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val project = ProjectDto("project-1", "user-a", "Build blocker", sourceClusterId = "42",
        stage = "watching", analysisStatus = "ready", buildStatus = "ready", approvalStatus = "draft")
    private fun fixture(name: String) = requireNotNull(javaClass.getResourceAsStream("/fixtures/" + name)).bufferedReader().use { it.readText() }

    @Test fun canonicalBackendAnalysisRetainsEvidenceAndGroundedClaims() {
        val dto = json.decodeFromString<AnalysisDto>(fixture("analysis-case.json"))
        val model = requireNotNull(AnalysisCacheDto(project, dto).toModel("user-a").analysis)
        assertEquals(WorkspaceStatus.READY, model.status)
        assertEquals("A supported thesis", model.thesis?.statement)
        assertEquals(101, model.evidence.single().id)
        assertEquals(listOf("101"), model.problems.single().evidenceIds)
        assertEquals("observed", model.claims.single().kind)
    }
    @Test fun canonicalBackendBuildRetainsPlanTasksTargetsAndEditableDocuments() {
        val dto = json.decodeFromString<SolutionDto>(fixture("solution-case.json"))
        val model = BuildCacheDto(project, dto).toModel("user-a").solution
        assertEquals(setOf(DeliveryTarget.WEB), model.targets)
        assertEquals("todo", model.tasks.single().status.name.lowercase())
        assertTrue(model.documents.any { it.key == "requirements" && it.editable })
        assertTrue(requireNotNull(model.architecture).stack.contains("FastAPI"))
        assertEquals(listOf("Send a reminder from an overdue invoice"), model.testPlan?.endToEnd)
    }
    @Test(expected = IllegalArgumentException::class)
    fun anUnknownClaimCategoryCannotMasqueradeAsAnInference() {
        val dto = json.decodeFromString<AnalysisDto>(fixture("analysis-case.json"))
        AnalysisCacheDto(project, dto.copy(claims = dto.claims.map { it.copy(kind = "unknown") })).toModel("user-a")
    }
    @Test(expected = IllegalArgumentException::class)
    fun danglingEvidenceLinksAreRejected() {
        val dto = json.decodeFromString<AnalysisDto>(fixture("analysis-case.json"))
        AnalysisCacheDto(project, dto.copy(evidence = emptyList())).toModel("user-a")
    }
    @Test(expected = IllegalArgumentException::class)
    fun incompleteReadyBuildCannotBeDisplayedAsSuccessful() {
        val dto = json.decodeFromString<SolutionDto>(fixture("solution-case.json"))
        BuildCacheDto(project, dto.copy(architecture = null)).toModel("user-a")
    }
    @Test(expected = IllegalArgumentException::class)
    fun projectFromAnotherAccountCannotEnterThisAccountsCache() {
        project.toModel("user-b")
    }
}
