package com.example.problemfinder.feature.opportunitydetails.impl

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import android.net.Uri
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalUriHandler
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.*
import androidx.navigation.compose.composable
import com.example.problemfinder.feature.opportunitydetails.api.OpportunityDetailsDestination as Routes
import com.example.problemfinder.feature.opportunitydetails.impl.details.*
import com.example.problemfinder.feature.opportunitydetails.impl.post.*
import com.example.problemfinder.feature.opportunitydetails.impl.util.messageRes

/** Evidence shares its parent Details VM; post loading is left to the post VM's logic TODO. */
fun NavGraphBuilder.opportunityDetails(navController: NavHostController, onMessage: (String) -> Unit) {
    val arguments = listOf(navArgument(Routes.ID_ARGUMENT) { type = NavType.IntType })
    listOf(Routes.ROUTE, Routes.EVIDENCE_ROUTE).forEach { route ->
        composable(route, arguments = arguments) { entry ->
            val clusterId = requireNotNull(entry.arguments).getInt(Routes.ID_ARGUMENT)
            val evidence = route == Routes.EVIDENCE_ROUTE
            val owner = if (evidence) remember(entry) {
                navController.getBackStackEntry(Routes.route(clusterId))
            } else entry
            val viewModel = hiltViewModel<OpportunityDetailsViewModel>(owner)
            val state = viewModel.state.collectAsStateWithLifecycle().value
            val resources = LocalResources.current
            LaunchedEffect(viewModel, clusterId) { viewModel.onIntent(OpportunityDetailsIntent.Entered(clusterId)) }
            LaunchedEffect(viewModel, entry) {
                entry.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                    viewModel.effects.collect { effect ->
                        when (effect) {
                            OpportunityDetailsEffect.NavigateBack -> navController.popBackStack()
                            is OpportunityDetailsEffect.NavigateToEvidence ->
                                navController.navigate(Routes.postRoute(clusterId, effect.evidence))
                            OpportunityDetailsEffect.NavigateToEvidenceList ->
                                if (!evidence) navController.navigate(Routes.evidenceRoute(clusterId))
                            is OpportunityDetailsEffect.ActionFailed ->
                                onMessage(resources.getString(effect.failure.messageRes().second))
                        }
                    }
                }
            }
            OpportunityDetailsScreen(state, viewModel::onIntent,
                section = if (evidence) DetailsSection.EVIDENCE else DetailsSection.OVERVIEW,
                onOverview = { if (evidence) navController.popBackStack() })
        }
    }
    composable(Routes.POST_ROUTE,
        arguments = arguments + navArgument(Routes.POST_ARGUMENT) { type = NavType.IntType },
    ) { entry ->
        val viewModel = hiltViewModel<PostDetailsViewModel>()
        val state = viewModel.state.collectAsStateWithLifecycle().value
        val uriHandler = LocalUriHandler.current
        val resources = LocalResources.current
        LaunchedEffect(viewModel) { viewModel.onIntent(PostDetailsIntent.Entered) }
        LaunchedEffect(viewModel, entry) {
            entry.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.effects.collect { effect ->
                    when (effect) {
                        PostDetailsEffect.NavigateBack -> navController.popBackStack()
                        is PostDetailsEffect.OpenExternal -> try {
                            // External platform navigation accepts web sources only.
                            if (Uri.parse(effect.url).scheme?.lowercase() in setOf("http", "https"))
                                uriHandler.openUri(effect.url)
                            else onMessage(resources.getString(R.string.post_link_failed))
                        } catch (_: Exception) { onMessage(resources.getString(R.string.post_link_failed)) }
                    }
                }
            }
        }
        PostDetailsScreen(state, viewModel::onIntent)
    }
}
