package com.example.problemfinder.feature.projects.impl

import com.example.problemfinder.core.mvi.BaseMVIViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** MVI scaffold for Projects; no feature actions or data operations are implemented yet. */
@HiltViewModel
class ProjectsViewModel @Inject constructor(): BaseMVIViewModel<ProjectsIntent, ProjectsState, ProjectsEffect>(ProjectsState()){
    override fun onIntent(intent: ProjectsIntent) {
        when (intent) {
            is ProjectsIntent.OpenProject -> sendEffect(ProjectsEffect.OpenProject(intent.id))
            ProjectsIntent.Discover -> sendEffect(ProjectsEffect.Discover)
            else -> {
                // TODO [ANALYZE-LOGIC] Observe account-owned pipeline projects; publish status/stage/cards.
                // Own stage selection, request handling and filtering here. No preview fixture is runtime data.
            }
        }
    }
    override fun onError(error: Throwable) {
        // TODO [ANALYZE-LOGIC] Publish a recoverable presentation failure.
    }
}
