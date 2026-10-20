package com.example.problemfinder.feature.opportunitydetails.impl

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.problemfinder.feature.opportunitydetails.api.OpportunityDetailsDestination

/** The app owns navigation; this adapter translates one-time feature effects into app actions. */
fun NavGraphBuilder.opportunityDetails(onBack: () -> Unit, onMessage: (String) -> Unit) {
    composable(
        route = OpportunityDetailsDestination.ROUTE,
        arguments = listOf(navArgument(OpportunityDetailsDestination.ID_ARGUMENT) { type = NavType.IntType }),
    ) { entry ->
        val clusterId = requireNotNull(entry.arguments).getInt(OpportunityDetailsDestination.ID_ARGUMENT)
        val viewModel = hiltViewModel<OpportunityDetailsViewModel>()
        val state = viewModel.state.collectAsStateWithLifecycle().value
        val context = LocalContext.current

        LaunchedEffect(viewModel, clusterId) {
            viewModel.onIntent(OpportunityDetailsIntent.Entered(clusterId))
            viewModel.effects.collect { effect ->
                when (effect) {
                    OpportunityDetailsEffect.NavigateBack -> onBack()
                    is OpportunityDetailsEffect.ActionFailed -> {
                        // TODO [DETAILS-6] Map the typed failure to localized actionable copy.
                        onMessage(context.getString(R.string.message_details_action_failed))
                    }
                    is OpportunityDetailsEffect.OpenEvidenceLink -> {
                        // TODO [DETAILS-6] Use LocalUriHandler or an app callback to open http/https
                        // links only. Validate the scheme and handle an unavailable browser as a
                        // user-visible failure. Keep URL opening out of Screen and ViewModel.
                    }
                }
            }
        }
        OpportunityDetailsScreen(state = state, onIntent = viewModel::onIntent)
    }
}
