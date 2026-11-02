package com.example.problemfinder.feature.builds.impl

import com.example.problemfinder.core.mvi.BaseMVIViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** MVI scaffold for Builds; no feature actions or data operations are implemented yet. */
@HiltViewModel
class BuildsViewModel @Inject constructor(): BaseMVIViewModel<BuildsIntent, BuildsState, BuildsEffect>(BuildsState()){
    override fun onIntent(intent: BuildsIntent) {
        when (intent) {
            is BuildsIntent.OpenProject -> sendEffect(BuildsEffect.OpenProject(intent.id))
            BuildsIntent.OpenAnalyze -> sendEffect(BuildsEffect.OpenAnalyze)
            else -> {
                // TODO [BUILD-LOGIC] Observe the account's build projects and publish cards/status.
            }
        }
    }
    override fun onError(error: Throwable) {
        // TODO [BUILD-LOGIC] Publish a recoverable presentation failure.
    }
}
