package com.example.problemfinder.feature.details.impl

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalUriHandler
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.problemfinder.feature.details.impl.details.DetailsSection
import com.example.problemfinder.feature.details.impl.details.OpportunityDetailsEffect
import com.example.problemfinder.feature.details.impl.details.OpportunityDetailsScreen
import com.example.problemfinder.feature.details.impl.details.OpportunityDetailsViewModel
import com.example.problemfinder.feature.details.impl.post.PostDetailsEffect
import com.example.problemfinder.feature.details.impl.post.PostDetailsIntent
import com.example.problemfinder.feature.details.impl.post.PostDetailsScreen
import com.example.problemfinder.feature.details.impl.post.PostDetailsViewModel
import com.example.problemfinder.feature.details.impl.util.messageRes
import com.example.problemfinder.feature.details.api.OpportunityDetailsDestination as Routes

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
    composable(
        Routes.POST_ROUTE,
        arguments = arguments + navArgument(Routes.POST_ARGUMENT) { type = NavType.StringType },
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
                            if (effect.url.toUri().scheme?.lowercase() in setOf("http", "https"))
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
