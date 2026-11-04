package com.example.problemfinder.feature.search.impl

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalResources
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.example.problemfinder.feature.search.api.SearchDestination

fun NavGraphBuilder.search(
    navController: NavHostController,
    onOpenCluster: (Int) -> Unit,
    onMessage: (String) -> Unit,
) {
    listOf(SearchDestination.ROUTE, SearchDestination.FILTERS, SearchDestination.ASSISTANT).forEach { route ->
        composable(route) { entry ->
            val owner = if (route == SearchDestination.ROUTE) entry else remember(entry) {
                navController.getBackStackEntry(SearchDestination.ROUTE)
            }
            val viewModel = hiltViewModel<SearchViewModel>(owner)
            val state = viewModel.state.collectAsStateWithLifecycle().value
            val resources = LocalResources.current
            LaunchedEffect(viewModel) { viewModel.onIntent(SearchIntent.Entered) }
            LaunchedEffect(viewModel, entry) {
                entry.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                    viewModel.effects.collect { effect ->
                        when (effect) {
                            is SearchEffect.NavigateToCluster -> onOpenCluster(effect.clusterId)
                            SearchEffect.NavigateToFilters -> navController.navigate(SearchDestination.FILTERS) { launchSingleTop = true }
                            SearchEffect.NavigateToAssistant -> navController.navigate(SearchDestination.ASSISTANT) { launchSingleTop = true }
                            SearchEffect.NavigateBack -> navController.popBackStack()
                            is SearchEffect.ActionFailed -> onMessage(resources.getString(R.string.message_search_action_failed))
                        }
                    }
                }
            }
            when (route) {
                SearchDestination.FILTERS -> SearchFiltersScreen(state, viewModel::onIntent)
                SearchDestination.ASSISTANT -> SearchAssistantScreen(state, viewModel::onIntent)
                else -> SearchScreen(state, viewModel::onIntent)
            }
        }
    }
}
