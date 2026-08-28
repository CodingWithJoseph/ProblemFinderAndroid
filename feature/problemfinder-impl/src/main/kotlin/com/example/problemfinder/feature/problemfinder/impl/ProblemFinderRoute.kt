package com.example.problemfinder.feature.problemfinder.impl

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.problemfinder.core.model.ProblemId
import com.example.problemfinder.feature.problemfinder.api.ProblemFinderDestination

fun NavGraphBuilder.problemFinderScreen(
    onProblemSelected: (ProblemId) -> Unit,
) {
    composable(route = ProblemFinderDestination.route) {
        ProblemFinderRoute(onProblemSelected = onProblemSelected)
    }
}

@Composable
fun ProblemFinderRoute(
    onProblemSelected: (ProblemId) -> Unit,
    viewModel: ProblemFinderViewModel = hiltViewModel(),
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value

    ProblemFinderScreen(
        state = state,
        onIntent = viewModel::onIntent,
    )

    // TODO: Collect viewModel.effectsWithLifecycle and route one-off effects here.
    @Suppress("UNUSED_VARIABLE")
    val reservedNavigationCallback = onProblemSelected
}
