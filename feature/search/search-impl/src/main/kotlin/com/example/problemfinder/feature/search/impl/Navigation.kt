package com.example.problemfinder.feature.search.impl

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.problemfinder.feature.search.api.SearchDestination

/** Search owns effects; AppNavigation owns the shared opportunity-details route and Snackbar. */
fun NavGraphBuilder.search(onOpenCluster: (Int) -> Unit, onMessage: (String) -> Unit) {
    composable(route = SearchDestination.ROUTE) {
        val viewModel = hiltViewModel<SearchViewModel>()
        val state = viewModel.state.collectAsStateWithLifecycle().value
        val context = LocalContext.current
        LaunchedEffect(viewModel) {
            viewModel.onIntent(SearchIntent.Entered)
            viewModel.effects.collect { effect ->
                when (effect) {
                    is SearchEffect.NavigateToCluster -> onOpenCluster(effect.clusterId)
                    is SearchEffect.ActionFailed -> {
                        // TODO [SEARCH-6] Map typed failures to localized actionable messages.
                        onMessage(context.getString(R.string.message_search_action_failed))
                    }
                }
            }
        }
        SearchScreen(state = state, onIntent = viewModel::onIntent)
    }
}
