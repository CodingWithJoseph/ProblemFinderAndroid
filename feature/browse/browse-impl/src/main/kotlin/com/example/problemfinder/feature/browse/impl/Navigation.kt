package com.example.problemfinder.feature.browse.impl

import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.problemfinder.feature.browse.api.BrowseDestination

fun NavGraphBuilder.browse() {
    composable(route = BrowseDestination.ROUTE) {
        val viewModel = hiltViewModel<BrowseViewModel>()
        val state = viewModel.state.collectAsStateWithLifecycle().value

        LaunchedEffect(viewModel) {}

        BrowseScreen()
    }
}
