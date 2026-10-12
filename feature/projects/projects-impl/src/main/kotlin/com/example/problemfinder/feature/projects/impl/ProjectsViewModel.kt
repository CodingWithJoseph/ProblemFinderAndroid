package com.example.problemfinder.feature.projects.impl

import com.example.problemfinder.core.mvi.BaseMVIViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ProjectsViewModel @Inject constructor(): BaseMVIViewModel<ProjectsIntent, ProjectsState, ProjectsEffect>(ProjectsState()){
    override fun onIntent(intent: ProjectsIntent) {}
    override fun onError(error: Throwable) {}
}
