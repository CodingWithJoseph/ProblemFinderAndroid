package com.example.problemfinder.feature.builds.impl

import androidx.lifecycle.SavedStateHandle
import com.example.problemfinder.core.mvi.BaseMVIViewModel
import com.example.problemfinder.feature.builds.api.BuildsDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** Routes work now. No generation, approval, task mutation, document persistence or export is implemented. */
@HiltViewModel
class BuildWorkspaceViewModel @Inject constructor(savedStateHandle: SavedStateHandle) :
    BaseMVIViewModel<BuildIntent, BuildWorkspaceState, BuildEffect>(
        BuildWorkspaceState(projectId = savedStateHandle[BuildsDestination.PROJECT_ID])
    ) {
    override fun onIntent(intent: BuildIntent) {
        when (intent) {
            BuildIntent.Back -> sendEffect(BuildEffect.Back)
            is BuildIntent.OpenFeature -> sendEffect(BuildEffect.OpenFeature(intent.id))
            is BuildIntent.OpenTask -> sendEffect(BuildEffect.OpenTask(intent.id))
            BuildIntent.OpenPlan -> sendEffect(BuildEffect.OpenPlan)
            BuildIntent.OpenDocuments -> sendEffect(BuildEffect.OpenDocuments)
            is BuildIntent.OpenDocument -> sendEffect(BuildEffect.OpenDocument(intent.id))
            BuildIntent.OpenAnalysis -> sendEffect(BuildEffect.OpenAnalysis)
            is BuildIntent.OpenOpportunity -> sendEffect(BuildEffect.OpenOpportunity(intent.clusterId))
            else -> {
                // TODO [BUILD-LOGIC] Observe projectId's versioned build plan and resolve Entered.itemId.
                // Supply selectedFeature/Task/Document and documentDraft; the UI does not select or derive them.
                // Implement generation, approval, task transitions, editing/save and export yourself.
                // Preserve existing documents on generation failure; enforce approved-version rules here.
            }
        }
    }
    override fun onError(error: Throwable) {
        // TODO [BUILD-LOGIC] Publish typed request failures without losing the last usable plan.
    }
}
