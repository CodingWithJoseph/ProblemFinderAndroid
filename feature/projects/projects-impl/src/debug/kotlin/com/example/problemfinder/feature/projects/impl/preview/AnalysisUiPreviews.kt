package com.example.problemfinder.feature.projects.impl.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.example.problemfinder.core.designsystem.components.*
import com.example.problemfinder.core.designsystem.theme.ImmensityTheme
import com.example.problemfinder.core.model.details.OpportunityEvidence
import com.example.problemfinder.feature.projects.impl.*
import com.example.problemfinder.core.model.workspace.*

/** Debug-only data for Compose previews. */
internal object AnalysisUiPreviewData {
    val evidence = OpportunityEvidence("101", "A workspace package cannot be resolved",
        "The project stops building after moving a shared package into the workspace.",
        null, "developer", "next.js", "GitHub", 18, 7, null, "Unclear package resolution failures block local development.")
    val problem = AnalysisProblem("problem-1", "Developers cannot identify the dependency that broke a build.",
        "The error describes a failed endpoint without naming the package or configuration responsible.",
        "Core problem", listOf("Small Next.js teams", "Developers maintaining shared workspaces"),
        "After updating dependencies or changing workspace structure.", listOf(evidence))
    val audience = AnalysisAudience("audience-1", "Developers maintaining growing workspaces",
        "Small teams whose local build configuration changes as shared packages grow.",
        listOf("It worked before the dependency update.", "Which package is the loader looking for?"),
        listOf("GitHub issues", "Next.js developer communities"),
        listOf("Ask maintainers about a recent build failure."),
        listOf("How often does this interrupt the same team?"))
    val workspace = AnalysisWorkspace("project-1", 42, "When the build becomes the blocker",
        "Understand the recurring failure before deciding what to build.", "Version 2 · updated today",
        "Small teams need actionable explanations when package resolution fails.",
        problems = listOf(problem), audiences = listOf(audience),
        assumptions = listOf(AnalysisAssumption("a-1", "Does the same failure regularly return?",
            "A recurring interruption may justify a dedicated solution.",
            "Interview teams about their last three build failures.")))
    val state = AnalysisWorkspaceState("project-1", workspace, problem, audience, FeatureContentStatus.CONTENT,
        turns = listOf(AnalysisTurn("1", "What does the evidence actually establish?", true),
            AnalysisTurn("2", "The posts show repeated resolution failures. Their frequency within a single team remains an assumption.", false)),
        userId = "preview-account", cacheReady = true, version = 2, caseStatus = WorkspaceStatus.READY)
    val home = ProjectsState(listOf(AnalysisProjectCard("project-1", workspace.title,
        workspace.summary, "Watching")), status = FeatureContentStatus.CONTENT)
}

@FeaturePreviews
@Composable private fun AnalyzeHomePreview() = ImmensityTheme { ProjectsScreen(AnalysisUiPreviewData.home, {}) }
@FeaturePreviews
@Composable private fun AnalysisOverviewPreview() = ImmensityTheme { AnalysisWorkspaceScreen(AnalysisUiPreviewData.state, {}) }
@FeaturePreviews
@Composable private fun AnalysisProblemPreview() = ImmensityTheme {
    AnalysisWorkspaceScreen(AnalysisUiPreviewData.state, {}, AnalysisPanel.PROBLEM)
}
@FeaturePreviews
@Composable private fun AnalysisAudiencePreview() = ImmensityTheme {
    AnalysisWorkspaceScreen(AnalysisUiPreviewData.state, {}, AnalysisPanel.AUDIENCE)
}
@FeaturePreviews
@Composable private fun AnalysisAssumptionsPreview() = ImmensityTheme {
    AnalysisWorkspaceScreen(AnalysisUiPreviewData.state, {}, AnalysisPanel.ASSUMPTIONS)
}
@FeaturePreviews
@Composable private fun AnalysisConversationPreview() = ImmensityTheme {
    AnalysisWorkspaceScreen(AnalysisUiPreviewData.state, {}, AnalysisPanel.CONVERSATION)
}

internal class AnalysisStatusPreviews : PreviewParameterProvider<FeatureContentStatus> {
    override val values = sequenceOf(FeatureContentStatus.EMPTY, FeatureContentStatus.LOADING, FeatureContentStatus.ERROR)
}
@Preview(name = "Analyze states", widthDp = 393, heightDp = 852)
@Composable private fun AnalysisStatePreview(@PreviewParameter(AnalysisStatusPreviews::class) status: FeatureContentStatus) =
    ImmensityTheme { ProjectsScreen(ProjectsState(status = status), {}) }
@Preview(name = "Analysis generating", widthDp = 393, heightDp = 852)
@Composable private fun AnalysisGeneratingPreview() = ImmensityTheme {
    AnalysisWorkspaceScreen(AnalysisWorkspaceState(status = FeatureContentStatus.LOADING,
        progressLabel = "Reviewing the supporting evidence…"), {})
}
