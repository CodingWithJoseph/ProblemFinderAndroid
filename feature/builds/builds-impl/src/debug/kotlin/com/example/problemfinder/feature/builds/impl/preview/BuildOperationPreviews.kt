package com.example.problemfinder.feature.builds.impl.preview

import androidx.compose.runtime.Composable
import com.example.problemfinder.core.designsystem.components.FeaturePreviews
import com.example.problemfinder.core.designsystem.theme.ImmensityTheme
import com.example.problemfinder.core.model.workspace.*
import com.example.problemfinder.feature.builds.impl.*

@FeaturePreviews
@Composable
private fun BuildGenerationOptionsPreview() = ImmensityTheme {
    BuildWorkspaceScreen(BuildWorkspaceState(projectId = "preview", cacheReady = true,
        deliveryTargets = setOf(DeliveryTarget.ANDROID), constraintsInput = "Keep the first release small"), {})
}
@FeaturePreviews
@Composable
private fun BuildTaskVerificationPreview() = ImmensityTheme {
    BuildWorkspaceScreen(BuildUiPreviewData.state.copy(
        workspace = BuildUiPreviewData.workspace.copy(approved = true),
        pendingTaskStatus = BuildTaskStatus.DONE, verificationSummary = "The focused tests passed.",
        verificationChecks = "gradlew test\nVerify the reproduction in the emulator",
    ), {}, BuildPanel.TASK)
}
@FeaturePreviews
@Composable
private fun BuildVersionConflictPreview() = ImmensityTheme {
    BuildWorkspaceScreen(BuildUiPreviewData.state.copy(
        workspace = BuildUiPreviewData.workspace.copy(version = 2),
        failure = WorkspaceFailure.CONFLICT, documentDirty = true, documentBaseVersion = 1,
        documentDraft = "# My local changes\n\nThese edits remain available after a version conflict.",
    ), {}, BuildPanel.DOCUMENT)
}
@FeaturePreviews
@Composable
private fun BuildUnsavedChangesPreview() = ImmensityTheme {
    BuildWorkspaceScreen(BuildUiPreviewData.state.copy(documentDirty = true,
        documentBaseVersion = 1, confirmLeave = true), {}, BuildPanel.DOCUMENT)
}
