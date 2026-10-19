package com.example.problemfinder.feature.opportunitydetails.impl

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.problemfinder.feature.opportunitydetails.api.OpportunityDetailsDestination

/** Reads the integer route argument and delegates back navigation to the app's owning NavController. */
fun NavGraphBuilder.opportunityDetails(onBack: () -> Unit) {
    composable(
        route = OpportunityDetailsDestination.ROUTE,
        arguments = listOf(navArgument(OpportunityDetailsDestination.ID_ARGUMENT) { type = NavType.IntType }),
    ) { entry ->
        val clusterId = requireNotNull(entry.arguments).getInt(OpportunityDetailsDestination.ID_ARGUMENT)
        OpportunityDetailsScreen(clusterId = clusterId, onBack = onBack)
    }
}
