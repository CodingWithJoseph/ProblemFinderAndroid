package com.example.problemfinder.feature.projects.impl.preview

import androidx.compose.runtime.Composable
import com.example.problemfinder.core.designsystem.components.FeaturePreviews
import com.example.problemfinder.core.designsystem.theme.ImmensityTheme
import com.example.problemfinder.core.model.workspace.*
import com.example.problemfinder.feature.projects.impl.*

@FeaturePreviews
@Composable
private fun AnalysisProposalPreview() = ImmensityTheme {
    AnalysisWorkspaceScreen(AnalysisUiPreviewData.state.copy(
        conversations = listOf(ConversationSummary("conversation-1", "Build failure investigation", false)),
        conversationId = "conversation-1",
        turns = listOf(AnalysisTurn("turn-1", "Focus the thesis on the package-resolution failure.",
            false, listOf(AnalysisCitation("101", "Source: package resolution failure")),
            AnalysisProposal("proposal-1", "Narrow the working thesis",
                "Focus on actionable dependency errors before considering automatic recovery.", "pending"))),
    ), {}, AnalysisPanel.CONVERSATION)
}
@FeaturePreviews
@Composable
private fun AnalysisSavedOfflinePreview() = ImmensityTheme {
    AnalysisWorkspaceScreen(AnalysisUiPreviewData.state.copy(failure = WorkspaceFailure.NETWORK), {})
}
