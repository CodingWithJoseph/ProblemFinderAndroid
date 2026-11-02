package com.example.problemfinder.feature.builds.impl

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.*
import androidx.navigation.compose.composable
import com.example.problemfinder.feature.builds.api.BuildsDestination as Routes
import com.example.problemfinder.core.model.workspace.WorkspaceExport
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun NavGraphBuilder.builds(
    navController: NavHostController,
    onOpenAnalysis: (String) -> Unit,
    onOpenAnalyze: () -> Unit,
    onOpenCluster: (Int) -> Unit,
) {
    composable(Routes.ROUTE) { entry ->
        val viewModel = hiltViewModel<BuildsViewModel>()
        val state = viewModel.state.collectAsStateWithLifecycle().value
        LaunchedEffect(viewModel, entry) {
            entry.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.onIntent(BuildsIntent.Entered)
                awaitCancellation()
            }
        }
        LaunchedEffect(viewModel, entry) {
            entry.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.effects.collect { effect ->
                    when (effect) {
                        is BuildsEffect.OpenProject -> navController.navigate(Routes.workspace(effect.id))
                        BuildsEffect.OpenAnalyze -> onOpenAnalyze()
                    }
                }
            }
        }
        BuildsScreen(state, viewModel::onIntent)
    }
    val panels = mapOf(
        Routes.WORKSPACE to BuildPanel.OVERVIEW, Routes.FEATURE to BuildPanel.FEATURE,
        Routes.TASK to BuildPanel.TASK, Routes.PLAN to BuildPanel.PLAN,
        Routes.DOCUMENTS to BuildPanel.DOCUMENTS, Routes.DOCUMENT to BuildPanel.DOCUMENT,
    )
    panels.forEach { (route, panel) ->
        composable(route,
            arguments = listOf(navArgument(Routes.PROJECT_ID) { type = NavType.StringType }) +
                if (panel in setOf(BuildPanel.FEATURE, BuildPanel.TASK, BuildPanel.DOCUMENT))
                    listOf(navArgument(Routes.ITEM_ID) { type = NavType.StringType }) else emptyList(),
        ) { entry ->
            val id = requireNotNull(entry.arguments?.getString(Routes.PROJECT_ID))
            val owner = if (panel == BuildPanel.OVERVIEW) entry else remember(entry) {
                navController.getBackStackEntry(Routes.workspace(id))
            }
            val viewModel = hiltViewModel<BuildWorkspaceViewModel>(owner)
            val state = viewModel.state.collectAsStateWithLifecycle().value
            val context = LocalContext.current
            val exportScope = rememberCoroutineScope()
            var pendingExport by remember(entry) { mutableStateOf<Pair<String, WorkspaceExport>?>(null) }
            val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
                val pending = pendingExport
                pendingExport = null
                if (uri != null && pending != null && viewModel.state.value.userId == pending.first) exportScope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            requireNotNull(context.contentResolver.openOutputStream(uri)).use { it.write(pending.second.content) }
                        }
                    } catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { viewModel.onIntent(BuildIntent.ExportFailed) }
                }
            }
            LaunchedEffect(viewModel, entry) {
                entry.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                    viewModel.onIntent(BuildIntent.Entered(entry.arguments?.getString(Routes.ITEM_ID), panel))
                    viewModel.onIntent(BuildIntent.VisibilityChanged(entry.id, true))
                    try { awaitCancellation() }
                    finally { viewModel.onIntent(BuildIntent.VisibilityChanged(entry.id, false)) }
                }
            }
            LaunchedEffect(viewModel, entry) {
                entry.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                    viewModel.effects.collect { effect ->
                        when (effect) {
                            is BuildEffect.ExportReady -> if (effect.userId == viewModel.state.value.userId) {
                                pendingExport = effect.userId to effect.export
                                try { exportLauncher.launch(effect.export.filename) }
                                catch (_: Exception) { pendingExport = null; viewModel.onIntent(BuildIntent.ExportFailed) }
                            }
                            BuildEffect.Back -> navController.popBackStack()
                            is BuildEffect.OpenFeature -> navController.navigate(Routes.feature(id, effect.id))
                            is BuildEffect.OpenTask -> navController.navigate(Routes.task(id, effect.id))
                            BuildEffect.OpenPlan -> navController.navigate(Routes.plan(id))
                            BuildEffect.OpenDocuments -> navController.navigate(Routes.documents(id))
                            is BuildEffect.OpenDocument -> navController.navigate(Routes.document(id, effect.id))
                            BuildEffect.OpenAnalysis -> onOpenAnalysis(id)
                            is BuildEffect.OpenOpportunity -> onOpenCluster(effect.clusterId)
                        }
                    }
                }
            }
            BuildWorkspaceScreen(state, viewModel::onIntent, panel)
        }
    }
}
