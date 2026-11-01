package com.example.problemfinder.feature.opportunities.impl

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalResources
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.problemfinder.core.common.shared.OpportunityCategoryFilter
import com.example.problemfinder.core.model.opportunity.OpportunityCategory
import com.example.problemfinder.feature.opportunities.api.OpportunitiesDestinations
import com.example.problemfinder.feature.opportunities.impl.browse.BrowseEffect
import com.example.problemfinder.feature.opportunities.impl.browse.BrowseIntent
import com.example.problemfinder.feature.opportunities.impl.browse.BrowseScreen
import com.example.problemfinder.feature.opportunities.impl.browse.BrowseViewModel
import com.example.problemfinder.feature.opportunities.impl.discovery.DiscoveryEffect
import com.example.problemfinder.feature.opportunities.impl.discovery.DiscoveryIntent
import com.example.problemfinder.feature.opportunities.impl.discovery.DiscoveryScreen
import com.example.problemfinder.feature.opportunities.impl.discovery.DiscoveryViewModel
import com.example.problemfinder.feature.opportunities.impl.util.messageRes

fun NavGraphBuilder.opportunities(
    onOpenCluster: (Int) -> Unit,
    onOpenBrowse: (OpportunityCategory) -> Unit,
    onMessage: (String) -> Unit,
) {
    composable(route = OpportunitiesDestinations.DISCOVERY) {
        val viewModel = hiltViewModel<DiscoveryViewModel>()
        val state = viewModel.state.collectAsStateWithLifecycle().value
        val context = LocalResources.current

        LaunchedEffect(viewModel) {
            viewModel.onIntent(DiscoveryIntent.Entered)

            viewModel.effects.collect {
                when (it) {
                    is DiscoveryEffect.NavigateToCluster -> onOpenCluster(it.clusterId)
                    is DiscoveryEffect.NavigateToBrowse -> onOpenBrowse(it.clusterCategory)
                    is DiscoveryEffect.ShowError -> onMessage(
                        context.getString(
                            R.string.message_watch_action_failed,
                            context.getString(it.error.messageRes().second)
                        )
                    )
                }
            }
        }

        DiscoveryScreen(
            state = state,
            onIntent = { viewModel.onIntent(it) }
        )
    }

    composable(
        route = OpportunitiesDestinations.BROWSE_ROUTE,
        arguments = listOf(
            navArgument(OpportunitiesDestinations.CATEGORY_ARGUMENT) {
                type = NavType.StringType
                defaultValue = OpportunityCategoryFilter.ALL.name
            }
        )
    ) {
        val viewModel = hiltViewModel<BrowseViewModel>()
        val state = viewModel.state.collectAsStateWithLifecycle().value
        val context = LocalResources.current

        LaunchedEffect(viewModel) {
            viewModel.onIntent(BrowseIntent.Entered)
            viewModel.effects.collect { effect ->
                when (effect) {
                    is BrowseEffect.ShowError -> onMessage(
                        context.getString(
                            R.string.message_watch_action_failed,
                            context.getString(
                                effect.error.messageRes().second
                            )
                        )
                    )

                    is BrowseEffect.NavigateToCluster -> onOpenCluster(effect.clusterId)
                }
            }
        }

        BrowseScreen(
            state = state,
            onIntent = { viewModel.onIntent(it) }
        )
    }
}
