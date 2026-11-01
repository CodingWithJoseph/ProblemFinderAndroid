package com.example.problemfinder.feature.opportunitydetails.impl

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalResources
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.problemfinder.feature.opportunitydetails.api.OpportunityDetailsDestination
import com.example.problemfinder.feature.opportunitydetails.impl.details.OpportunityDetailsEffect
import com.example.problemfinder.feature.opportunitydetails.impl.details.OpportunityDetailsIntent
import com.example.problemfinder.feature.opportunitydetails.impl.details.OpportunityDetailsScreen
import com.example.problemfinder.feature.opportunitydetails.impl.details.OpportunityDetailsViewModel
import com.example.problemfinder.feature.opportunitydetails.impl.util.messageRes

fun NavGraphBuilder.opportunityDetails(
    onBack: () -> Unit,
    onOpenEvidence: (Int) -> Unit,
    onMessage: (String) -> Unit
) {
    composable(
        route = OpportunityDetailsDestination.ROUTE,
        arguments = listOf(navArgument(OpportunityDetailsDestination.ID_ARGUMENT) {
            type = NavType.IntType
        }),
    ) { entry ->
        val clusterId =
            requireNotNull(entry.arguments).getInt(OpportunityDetailsDestination.ID_ARGUMENT)
        val viewModel = hiltViewModel<OpportunityDetailsViewModel>()
        val state = viewModel.state.collectAsStateWithLifecycle().value
        val context = LocalResources.current

        LaunchedEffect(viewModel, clusterId) {
            viewModel.onIntent(OpportunityDetailsIntent.Entered(clusterId))
            viewModel.effects.collect { effect ->
                when (effect) {
                    OpportunityDetailsEffect.NavigateBack -> onBack()
                    is OpportunityDetailsEffect.ActionFailed -> onMessage(context.getString(effect.failure.messageRes().second))
                    is OpportunityDetailsEffect.NavigateToEvidence -> onOpenEvidence(effect.evidence)
                }
            }
        }
        OpportunityDetailsScreen(state = state, onIntent = viewModel::onIntent)
    }
}
