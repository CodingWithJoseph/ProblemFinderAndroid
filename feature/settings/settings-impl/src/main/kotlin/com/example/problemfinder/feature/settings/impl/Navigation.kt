package com.example.problemfinder.feature.settings.impl

import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.problemfinder.feature.settings.api.SettingsDestination

fun NavGraphBuilder.settings(
    onClose: () -> Unit
) {
    composable(SettingsDestination.ROUTE) {
        val viewModel = hiltViewModel<SettingsViewModel>()
        val state = viewModel.state.collectAsStateWithLifecycle().value

        LaunchedEffect(viewModel) { }
        SettingsScreen(
            onClose = onClose
        )
    }
}
