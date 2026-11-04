package com.example.problemfinder.feature.settings.impl

import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.problemfinder.feature.settings.api.SettingsDestination

fun NavGraphBuilder.settings(
    onClose: () -> Unit,
    onSignOut: () -> Unit,
    appVersion: String,
    isSigningOut: () -> Boolean,
) {
    composable(SettingsDestination.ROUTE) { entry ->
        val viewModel = hiltViewModel<SettingsViewModel>()
        val state = viewModel.state.collectAsStateWithLifecycle().value
        LaunchedEffect(viewModel, entry) {
            entry.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.effects.collect { if (it == SettingsEffect.SignOut) onSignOut() }
            }
        }
        SettingsScreen(onClose, state = state, onIntent = viewModel::onIntent,
            appVersion = appVersion, isSigningOut = isSigningOut())
    }
}
