package com.example.problemfinder.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.example.problemfinder.core.model.ProblemId
import com.example.problemfinder.feature.problemfinder.api.ProblemFinderDestination
import com.example.problemfinder.feature.problemfinder.impl.problemFinderScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = ProblemFinderDestination.route,
    ) {
        problemFinderScreen(
            onProblemSelected = { problemId ->
                // TODO: Navigate after a problem-details destination exists.
                @Suppress("UNUSED_VARIABLE")
                val selectedProblemId: ProblemId = problemId
            },
        )
    }
}
