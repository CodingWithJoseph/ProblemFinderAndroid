package com.example.problemfinder.feature.projects.impl

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.*
import androidx.navigation.compose.composable
import com.example.problemfinder.feature.projects.api.ProjectsDestination as Routes

fun NavGraphBuilder.projects(
    navController: NavHostController,
    onOpenCluster: (Int) -> Unit,
    onOpenPost: (Int, Int) -> Unit,
    onOpenBuild: (String) -> Unit,
    onDiscover: () -> Unit,
) {
    composable(Routes.ROUTE) { entry ->
        val viewModel = hiltViewModel<ProjectsViewModel>()
        val state = viewModel.state.collectAsStateWithLifecycle().value
        LaunchedEffect(viewModel) { viewModel.onIntent(ProjectsIntent.Entered) }
        LaunchedEffect(viewModel, entry) {
            entry.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.effects.collect { effect ->
                    when (effect) {
                        is ProjectsEffect.OpenProject -> navController.navigate(Routes.workspace(effect.id))
                        ProjectsEffect.Discover -> onDiscover()
                    }
                }
            }
        }
        ProjectsScreen(state, viewModel::onIntent)
    }
    val panels = mapOf(
        Routes.WORKSPACE to AnalysisPanel.OVERVIEW, Routes.PROBLEM to AnalysisPanel.PROBLEM,
        Routes.AUDIENCE to AnalysisPanel.AUDIENCE, Routes.ASSUMPTIONS to AnalysisPanel.ASSUMPTIONS,
        Routes.CONVERSATION to AnalysisPanel.CONVERSATION,
    )
    panels.forEach { (route, panel) ->
        composable(
            route, arguments = listOf(navArgument(Routes.PROJECT_ID) { type = NavType.StringType }) +
                if (panel == AnalysisPanel.PROBLEM || panel == AnalysisPanel.AUDIENCE)
                    listOf(navArgument(Routes.ITEM_ID) { type = NavType.StringType }) else emptyList(),
        ) { entry ->
            val id = requireNotNull(entry.arguments?.getString(Routes.PROJECT_ID))
            val owner = if (panel == AnalysisPanel.OVERVIEW) entry else remember(entry) {
                navController.getBackStackEntry(Routes.workspace(id))
            }
            val viewModel = hiltViewModel<AnalysisWorkspaceViewModel>(owner)
            val state = viewModel.state.collectAsStateWithLifecycle().value
            LaunchedEffect(viewModel, entry) {
                viewModel.onIntent(AnalysisIntent.Entered(entry.arguments?.getString(Routes.ITEM_ID), panel))
            }
            LaunchedEffect(viewModel, entry) {
                entry.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                    viewModel.effects.collect { effect ->
                        when (effect) {
                            AnalysisEffect.Back -> navController.popBackStack()
                            is AnalysisEffect.OpenProblem -> navController.navigate(Routes.problem(id, effect.id))
                            is AnalysisEffect.OpenAudience -> navController.navigate(Routes.audience(id, effect.id))
                            AnalysisEffect.OpenAssumptions -> navController.navigate(Routes.assumptions(id))
                            AnalysisEffect.OpenConversation -> navController.navigate(Routes.conversation(id))
                            is AnalysisEffect.OpenOpportunity -> onOpenCluster(effect.clusterId)
                            is AnalysisEffect.OpenPost -> onOpenPost(effect.clusterId, effect.postId)
                            AnalysisEffect.OpenBuild -> onOpenBuild(id)
                        }
                    }
                }
            }
            AnalysisWorkspaceScreen(state, viewModel::onIntent, panel)
        }
    }
}
