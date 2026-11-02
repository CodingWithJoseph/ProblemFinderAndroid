package com.example.problemfinder.feature.builds.impl

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.problemfinder.core.designsystem.components.*
import com.example.problemfinder.core.designsystem.R as DesignR

@Composable
fun BuildsScreen(state: BuildsState, onIntent: (BuildsIntent) -> Unit, modifier: Modifier = Modifier) {
    FeatureScaffold(stringResource(R.string.build_title), modifier,
        onRefresh = { onIntent(BuildsIntent.Reload) }, refreshing = state.status == FeatureContentStatus.LOADING,
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)) {
            item { FeaturePageHeader(stringResource(R.string.build_eyebrow), stringResource(R.string.build_headline),
                stringResource(R.string.build_subtitle)) }
            if (state.projects.isEmpty()) item {
                when (state.status) {
                    FeatureContentStatus.LOADING -> FeatureLoading(stringResource(R.string.build_loading))
                    FeatureContentStatus.ERROR -> FeatureNotice(stringResource(R.string.build_error),
                        stringResource(R.string.build_error_message), { onIntent(BuildsIntent.Reload) })
                    else -> FeatureEmpty(stringResource(R.string.build_empty), stringResource(R.string.build_empty_message),
                        DesignR.drawable.icon_builds, stringResource(R.string.build_open_analyze), { onIntent(BuildsIntent.OpenAnalyze) })
                }
            }
            items(state.projects, key = { it.id }) { project ->
                FeatureEntry(project.title, project.summary, { onIntent(BuildsIntent.OpenProject(project.id)) },
                    icon = DesignR.drawable.icon_builds, status = project.statusLabel)
            }
            if (state.projects.isNotEmpty() && state.status == FeatureContentStatus.ERROR) item("cached-error") {
                FeatureNotice(stringResource(R.string.build_error), stringResource(R.string.build_error_message),
                    { onIntent(BuildsIntent.Reload) })
            }
        }
    }
}

@Composable
internal fun BuildWorkspaceScreen(
    state: BuildWorkspaceState,
    onIntent: (BuildIntent) -> Unit,
    panel: BuildPanel = BuildPanel.OVERVIEW,
) {
    FeatureScaffold(
        stringResource(when (panel) {
            BuildPanel.OVERVIEW -> R.string.build_workspace
            BuildPanel.FEATURE -> R.string.build_feature
            BuildPanel.TASK -> R.string.build_task
            BuildPanel.PLAN -> R.string.build_plan
            BuildPanel.DOCUMENTS -> R.string.build_documents
            BuildPanel.DOCUMENT -> R.string.build_document
        }),
        onBack = { onIntent(BuildIntent.Back) },
        onRefresh = { onIntent(BuildIntent.Reload) },
        refreshing = state.status == FeatureContentStatus.LOADING,
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)) {
            if (state.status == FeatureContentStatus.ERROR) item("error") {
                FeatureNotice(stringResource(R.string.build_error), stringResource(R.string.build_error_message),
                    { onIntent(BuildIntent.Reload) })
            }
            if (state.status == FeatureContentStatus.LOADING) item("loading") {
                FeatureLoading(state.progressLabel.ifEmpty { stringResource(R.string.build_generating) })
            }
            when (panel) {
                BuildPanel.OVERVIEW -> {
                    val workspace = state.workspace
                    if (workspace == null && state.status != FeatureContentStatus.LOADING) item("empty") {
                        FeatureEmpty(stringResource(R.string.build_start), stringResource(R.string.build_start_message),
                            DesignR.drawable.icon_builds, stringResource(R.string.build_generate), { onIntent(BuildIntent.Generate) })
                    }
                    if (workspace != null) {
                        item("hero") { FeatureHero(stringResource(R.string.build_scope_eyebrow), workspace.title,
                            workspace.goal, icon = DesignR.drawable.icon_builds) {
                            Text(workspace.versionLabel, style = MaterialTheme.typography.bodySmall)
                            Text(workspace.approvalLabel, style = MaterialTheme.typography.labelLarge)
                            FeatureAction(stringResource(R.string.build_approve),
                                { onIntent(BuildIntent.Approve) }, icon = DesignR.drawable.icon_check, prominent = true)
                        } }
                        item("users-title") { FeatureSection(stringResource(R.string.build_users)) }
                        items(workspace.users, key = { "user-" + it }) { FeatureBullet(it) }
                        item("flow-title") { FeatureSection(stringResource(R.string.build_flow)) }
                        items(workspace.primaryFlow, key = { "flow-" + it }) { FeatureBullet(it) }
                        item("features-title") { FeatureSection(stringResource(R.string.build_features)) }
                        items(workspace.features, key = { "feature-" + it.id }) { feature ->
                            FeatureEntry(feature.title, feature.summary, { onIntent(BuildIntent.OpenFeature(feature.id)) },
                                icon = DesignR.drawable.icon_lightbulb)
                        }
                        item("tasks-title") { FeatureSection(stringResource(R.string.build_tasks)) }
                        items(workspace.tasks, key = { "task-" + it.id }) { task ->
                            FeatureEntry(task.title, task.description, { onIntent(BuildIntent.OpenTask(task.id)) },
                                icon = DesignR.drawable.icon_check_circle, status = taskStatusLabel(task.status))
                        }
                        item("plan") { FeatureEntry(stringResource(R.string.build_plan),
                            stringResource(R.string.build_plan_subtitle), { onIntent(BuildIntent.OpenPlan) },
                            icon = DesignR.drawable.icon_workflow) }
                        item("documents") { FeatureEntry(stringResource(R.string.build_documents),
                            stringResource(R.string.build_documents_subtitle), { onIntent(BuildIntent.OpenDocuments) },
                            icon = DesignR.drawable.icon_document) }
                        item("analysis") { FeatureEntry(stringResource(R.string.build_view_analysis),
                            stringResource(R.string.build_view_analysis_subtitle), { onIntent(BuildIntent.OpenAnalysis) },
                            icon = DesignR.drawable.icon_target) }
                        item("opportunity") { FeatureEntry(stringResource(R.string.build_view_opportunity),
                            stringResource(R.string.build_view_opportunity_subtitle),
                            { onIntent(BuildIntent.OpenOpportunity(workspace.clusterId)) },
                            icon = DesignR.drawable.icon_arrow_up_right) }
                        item("export") { FeatureAction(stringResource(R.string.build_export),
                            { onIntent(BuildIntent.Export) }, Modifier.fillMaxWidth(), DesignR.drawable.icon_arrow_up_right) }
                    }
                }
                BuildPanel.FEATURE -> {
                    val feature = state.selectedFeature
                    if (feature == null) item { BuildSelectionMissing { onIntent(BuildIntent.Reload) } }
                    else {
                        item { FeaturePageHeader(stringResource(R.string.build_feature_eyebrow), feature.title, feature.summary) }
                        item { FeatureSection(stringResource(R.string.build_acceptance)) }
                        items(feature.acceptanceCriteria, key = { it }) { FeatureBullet(it) }
                        item { FeatureAction(stringResource(R.string.build_view_analysis),
                            { onIntent(BuildIntent.OpenAnalysis) }, Modifier.fillMaxWidth(), DesignR.drawable.icon_target) }
                    }
                }
                BuildPanel.TASK -> {
                    val task = state.selectedTask
                    if (task == null) item { BuildSelectionMissing { onIntent(BuildIntent.Reload) } }
                    else {
                        item { FeaturePageHeader(stringResource(R.string.build_task_eyebrow), task.title, task.description) }
                        item {
                            FeatureSection(stringResource(R.string.build_task_status))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                BuildTaskStatus.entries.forEach { status ->
                                    FilterChip(task.status == status, { onIntent(BuildIntent.TaskStatusSelected(status)) },
                                        label = { Text(taskStatusLabel(status)) })
                                }
                            }
                        }
                        item { FeatureSection(stringResource(R.string.build_acceptance)) }
                        items(task.acceptanceChecks, key = { "check-" + it }) { FeatureBullet(it) }
                        if (task.dependencies.isNotEmpty()) item { FeatureSection(stringResource(R.string.build_dependencies)) }
                        items(task.dependencies, key = { "dependency-" + it }) { FeatureBullet(it) }
                    }
                }
                BuildPanel.PLAN -> {
                    item { FeaturePageHeader(stringResource(R.string.build_delivery_eyebrow),
                        stringResource(R.string.build_delivery_title), stringResource(R.string.build_plan_subtitle)) }
                    val workspace = state.workspace
                    if (workspace == null) item { BuildSelectionMissing { onIntent(BuildIntent.Reload) } }
                    else {
                        item { FeatureSection(stringResource(R.string.build_constraints)) }
                        items(workspace.constraints, key = { "constraint-" + it }) { FeatureBullet(it) }
                        item { FeatureSection(stringResource(R.string.build_non_goals)) }
                        items(workspace.nonGoals, key = { "non-goal-" + it }) { FeatureBullet(it) }
                        items(workspace.plan, key = { it.id }) { section ->
                            FeaturePanel {
                                FeatureSection(section.title)
                                Text(section.body, style = MaterialTheme.typography.bodyMedium)
                                section.items.forEach { FeatureBullet(it) }
                            }
                        }
                    }
                }
                BuildPanel.DOCUMENTS -> {
                    item { FeaturePageHeader(stringResource(R.string.build_documents_eyebrow),
                        stringResource(R.string.build_documents), stringResource(R.string.build_documents_subtitle)) }
                    if (state.workspace?.documents.isNullOrEmpty()) item {
                        FeatureEmpty(stringResource(R.string.build_no_documents),
                            stringResource(R.string.build_no_documents_message), DesignR.drawable.icon_document)
                    }
                    items(state.workspace?.documents.orEmpty(), key = { it.id }) { document ->
                        FeatureEntry(document.title, document.filename, { onIntent(BuildIntent.OpenDocument(document.id)) },
                            icon = DesignR.drawable.icon_document)
                    }
                    item { FeatureAction(stringResource(R.string.build_export),
                        { onIntent(BuildIntent.Export) }, Modifier.fillMaxWidth(), DesignR.drawable.icon_arrow_up_right) }
                }
                BuildPanel.DOCUMENT -> {
                    val document = state.selectedDocument
                    if (document == null) item { BuildSelectionMissing { onIntent(BuildIntent.Reload) } }
                    else {
                        item { FeaturePageHeader(stringResource(R.string.build_document_eyebrow), document.title, document.filename) }
                        item {
                            if (document.editable) OutlinedTextField(
                                state.documentDraft, { onIntent(BuildIntent.DocumentChanged(it)) },
                                Modifier.fillMaxWidth(), minLines = 12, textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                                label = { Text(stringResource(R.string.build_document_content)) },
                            ) else SelectionContainer {
                                Text(document.content, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace))
                            }
                        }
                        if (document.editable) item { FeatureAction(stringResource(R.string.build_save),
                            { onIntent(BuildIntent.SaveDocument) }, Modifier.fillMaxWidth(), DesignR.drawable.icon_check,
                            pending = state.isSaving) }
                    }
                }
            }
        }
    }
}

@Composable
private fun taskStatusLabel(status: BuildTaskStatus): String = stringResource(when (status) {
    BuildTaskStatus.TODO -> R.string.build_status_todo
    BuildTaskStatus.IN_PROGRESS -> R.string.build_status_in_progress
    BuildTaskStatus.BLOCKED -> R.string.build_status_blocked
    BuildTaskStatus.DONE -> R.string.build_status_done
})

@Composable
private fun BuildSelectionMissing(onRetry: () -> Unit) {
    FeatureEmpty(stringResource(R.string.build_selection_missing),
        stringResource(R.string.build_selection_missing_message), DesignR.drawable.icon_document,
        stringResource(DesignR.string.ui_retry), onRetry)
}
