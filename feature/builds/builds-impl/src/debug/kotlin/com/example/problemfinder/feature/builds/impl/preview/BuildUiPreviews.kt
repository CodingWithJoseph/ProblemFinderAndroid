package com.example.problemfinder.feature.builds.impl.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.example.problemfinder.core.designsystem.components.*
import com.example.problemfinder.core.designsystem.theme.ImmensityTheme
import com.example.problemfinder.feature.builds.impl.*

/** Debug-only data for Compose previews. */
internal object BuildUiPreviewData {
    val feature = BuildFeature("feature-1", "Explain the failed dependency",
        "Turn a package resolution failure into an actionable explanation.",
        listOf("Identify the dependency involved.", "Link the explanation to the relevant configuration."))
    val task = BuildTask("task-1", "Capture a reproducible build failure",
        "Create a small workspace that reproduces the dependency-resolution error.",
        BuildTaskStatus.IN_PROGRESS, listOf("A clean checkout reproduces the failure.", "The expected error is documented."),
        listOf("Agree on the supported Next.js version."))
    val document = BuildDocument("build_contract", "Build contract", "BUILD_CONTRACT.md",
        "# Build contract\n\nGoal: explain one reproducible dependency-resolution failure.\n\n" +
            "Users: developers maintaining shared workspaces.\n\nOut of scope: automatically editing project configuration.",
        editable = true)
    val workspace = BuildWorkspace("project-1", 42, "Build failure explainer",
        "Help a developer identify and resolve one package-resolution failure.",
        "Version 1 · draft", "Awaiting your approval",
        users = listOf("Developers maintaining shared workspaces"),
        constraints = listOf("Keep the first version read-only.", "Support one reproducible failure."),
        nonGoals = listOf("Automatically rewriting project configuration."),
        primaryFlow = listOf("Provide the build error.", "Review the identified dependency.", "Follow the suggested checks."),
        features = listOf(feature), tasks = listOf(task),
        plan = listOf(
            BuildPlanSection("architecture", "Architecture", "Keep evidence collection separate from explanation.",
                listOf("Android: Compose, ViewModel, repository.", "Backend: validated explanation contract.")),
            BuildPlanSection("tests", "Test plan", "Verify the primary failure and recovery paths.",
                listOf("Malformed input remains actionable.", "A missing dependency produces an explanation.")),
            BuildPlanSection("delivery", "Delivery checklist", "Prepare a small, reviewable first release.",
                listOf("Confirm the reproducible example.", "Review source attribution.", "Verify accessibility."))),
        documents = listOf(document, document.copy(id = "requirements", title = "Requirements", filename = "REQUIREMENTS.md")))
    val state = BuildWorkspaceState("project-1", workspace, feature, task, document, document.content,
        FeatureContentStatus.CONTENT)
    val home = BuildsState(listOf(BuildProjectCard("project-1", workspace.title, workspace.goal, "Draft")),
        FeatureContentStatus.CONTENT)
}

@FeaturePreviews
@Composable private fun BuildHomePreview() = ImmensityTheme { BuildsScreen(BuildUiPreviewData.home, {}) }
@FeaturePreviews
@Composable private fun BuildOverviewPreview() = ImmensityTheme { BuildWorkspaceScreen(BuildUiPreviewData.state, {}) }
@FeaturePreviews
@Composable private fun BuildFeaturePreview() = ImmensityTheme {
    BuildWorkspaceScreen(BuildUiPreviewData.state, {}, BuildPanel.FEATURE)
}
@FeaturePreviews
@Composable private fun BuildTaskPreview() = ImmensityTheme {
    BuildWorkspaceScreen(BuildUiPreviewData.state, {}, BuildPanel.TASK)
}
@FeaturePreviews
@Composable private fun BuildPlanPreview() = ImmensityTheme {
    BuildWorkspaceScreen(BuildUiPreviewData.state, {}, BuildPanel.PLAN)
}
@FeaturePreviews
@Composable private fun BuildDocumentsPreview() = ImmensityTheme {
    BuildWorkspaceScreen(BuildUiPreviewData.state, {}, BuildPanel.DOCUMENTS)
}
@FeaturePreviews
@Composable private fun BuildDocumentPreview() = ImmensityTheme {
    BuildWorkspaceScreen(BuildUiPreviewData.state, {}, BuildPanel.DOCUMENT)
}

internal class BuildStatusPreviews : PreviewParameterProvider<FeatureContentStatus> {
    override val values = sequenceOf(FeatureContentStatus.EMPTY, FeatureContentStatus.LOADING, FeatureContentStatus.ERROR)
}
@Preview(name = "Build states", widthDp = 393, heightDp = 852)
@Composable private fun BuildStatePreview(@PreviewParameter(BuildStatusPreviews::class) status: FeatureContentStatus) =
    ImmensityTheme { BuildsScreen(BuildsState(status = status), {}) }
@Preview(name = "Build generating", widthDp = 393, heightDp = 852)
@Composable private fun BuildGeneratingPreview() = ImmensityTheme {
    BuildWorkspaceScreen(BuildWorkspaceState(status = FeatureContentStatus.LOADING,
        progressLabel = "Checking the solution against its evidence…"), {})
}
