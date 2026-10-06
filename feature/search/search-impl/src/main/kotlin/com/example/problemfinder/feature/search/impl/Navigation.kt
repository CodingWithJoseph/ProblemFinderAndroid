package com.example.problemfinder.feature.search.impl

import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.problemfinder.feature.search.api.SearchDestination

fun NavGraphBuilder.search() {
    composable(route = SearchDestination.ROUTE) {
        val viewModel = hiltViewModel<SearchViewModel>()
        val state = viewModel.state.collectAsStateWithLifecycle().value

        LaunchedEffect(viewModel) {
            // Effects go here
        }

        SearchScreen()
    }
}
