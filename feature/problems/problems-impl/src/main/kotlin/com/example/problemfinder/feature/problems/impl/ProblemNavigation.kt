package com.example.problemfinder.feature.problems.impl

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.problemfinder.feature.problems.api.ProblemsDestination

fun NavGraphBuilder.problemsScreen(
    isSigningOut: () -> Boolean,
    signOutFailed: () -> Boolean,
    onSignOut: () -> Unit
) {
    composable(route = ProblemsDestination.PROBLEMS) {
        ProblemsScreen(
            isSigningOut = isSigningOut(),
            signOutFailed = signOutFailed(),
            onSignOut = onSignOut
        )
    }
}
