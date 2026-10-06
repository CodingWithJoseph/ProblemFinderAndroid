package com.example.problemfinder.feature.builds.impl

import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.problemfinder.feature.builds.api.BuildsDestination

fun NavGraphBuilder.builds() {
    composable(route = BuildsDestination.ROUTE) {
        val viewModel = hiltViewModel<BuildsViewModel>()
        val state = viewModel.state.collectAsStateWithLifecycle().value

        LaunchedEffect(viewModel) {
        }

        BuildsScreen()
    }
}
