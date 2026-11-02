package com.example.problemfinder.feature.projects.impl

import androidx.lifecycle.SavedStateHandle
import com.example.problemfinder.core.mvi.BaseMVIViewModel
import com.example.problemfinder.feature.projects.api.ProjectsDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AnalysisWorkspaceViewModel @Inject constructor(savedStateHandle: SavedStateHandle) :
    BaseMVIViewModel<AnalysisIntent, AnalysisWorkspaceState, AnalysisEffect>(
        AnalysisWorkspaceState(projectId = savedStateHandle[ProjectsDestination.PROJECT_ID])
    ) {
    override fun onIntent(intent: AnalysisIntent) {
        when (intent) {
            AnalysisIntent.Back -> sendEffect(AnalysisEffect.Back)
            is AnalysisIntent.OpenProblem -> sendEffect(AnalysisEffect.OpenProblem(intent.id))
            is AnalysisIntent.OpenAudience -> sendEffect(AnalysisEffect.OpenAudience(intent.id))
            AnalysisIntent.OpenAssumptions -> sendEffect(AnalysisEffect.OpenAssumptions)
            AnalysisIntent.OpenConversation -> sendEffect(AnalysisEffect.OpenConversation)
            is AnalysisIntent.OpenOpportunity -> sendEffect(AnalysisEffect.OpenOpportunity(intent.clusterId))
            is AnalysisIntent.OpenPost -> sendEffect(AnalysisEffect.OpenPost(intent.clusterId, intent.postId))
            AnalysisIntent.OpenBuild -> sendEffect(AnalysisEffect.OpenBuild)
            else -> {
                // TODO [ANALYZE-LOGIC] Load/observe projectId's analysis and resolve Entered.itemId.
                // Publish workspace, selectedProblem/selectedAudience, progress and conversation state.
                // Implement generation, confirmation, pin/reject, assumption edits and sending yourself.
                // Keep generated claims distinct from confirmed facts; persist only confirmed mutations.
            }
        }
    }
    override fun onError(error: Throwable) {
        // TODO [ANALYZE-LOGIC] Handle observation/operation errors with account and request ownership.
    }
}
