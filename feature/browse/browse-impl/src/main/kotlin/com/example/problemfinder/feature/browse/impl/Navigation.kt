package com.example.problemfinder.feature.browse.impl

import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.problemfinder.feature.browse.api.BrowseDestination
import com.example.problemfinder.feature.browse.impl.browse.BrowseEffect
import com.example.problemfinder.feature.browse.impl.browse.BrowseIntent
import com.example.problemfinder.feature.browse.impl.browse.BrowseScreen
import com.example.problemfinder.feature.browse.impl.browse.BrowseViewModel

fun NavGraphBuilder.browse() {
    composable(route = BrowseDestination.ROUTE) {
        val viewModel = hiltViewModel<BrowseViewModel>()
        val state = viewModel.state.collectAsStateWithLifecycle().value

        LaunchedEffect(viewModel) {
            viewModel.onIntent(BrowseIntent.Entered)
        }

        BrowseScreen(
            state = state,
            onIntent = { viewModel.onIntent(it) }
        )
    }
}
