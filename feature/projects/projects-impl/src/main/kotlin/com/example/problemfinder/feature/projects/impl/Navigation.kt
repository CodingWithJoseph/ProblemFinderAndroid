package com.example.problemfinder.feature.projects.impl

import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.problemfinder.feature.projects.api.ProjectsDestination

/** Registers the placeholder destination; the reserved ViewModel state/effects do not drive UI yet. */
fun NavGraphBuilder.projects() {
    composable(route = ProjectsDestination.ROUTE) {
        val viewModel = hiltViewModel<ProjectsViewModel>()
        val state = viewModel.state.collectAsStateWithLifecycle().value

        LaunchedEffect(viewModel) {
            // Effects
        }

        ProjectsScreen()
    }
}
