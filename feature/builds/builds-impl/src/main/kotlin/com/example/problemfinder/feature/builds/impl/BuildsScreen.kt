package com.example.problemfinder.feature.builds.impl

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.activity.compose.BackHandler
import com.example.problemfinder.core.model.workspace.DeliveryTarget
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.problemfinder.core.designsystem.components.*
import com.example.problemfinder.core.designsystem.R as DesignR

@Composable
fun BuildsScreen(state: BuildsState, onIntent: (BuildsIntent) -> Unit, modifier: Modifier = Modifier) {
    FeatureScaffold(stringResource(R.string.build_title), modifier,
        onRefresh = { onIntent(BuildsIntent.Reload) }, refreshing = state.isRefreshing,
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).imePadding(), contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)) {
            item { FeaturePageHeader(stringResource(R.string.build_eyebrow), stringResource(R.string.build_headline),
                stringResource(R.string.build_subtitle)) }
            if (state.projects.isEmpty()) item {
                when (state.status) {
                    FeatureContentStatus.LOADING -> FeatureLoading(stringResource(R.string.build_loading))
                    FeatureContentStatus.ERROR -> FeatureNotice(stringResource(R.string.build_error),
                        state.failure?.let { workspaceFailureMessage(it) } ?: stringResource(R.string.build_error_message), { onIntent(BuildsIntent.Reload) })
                    else -> FeatureEmpty(stringResource(R.string.build_empty), stringResource(R.string.build_empty_message),
                        DesignR.drawable.icon_builds, stringResource(R.string.build_open_analyze), { onIntent(BuildsIntent.OpenAnalyze) })
                }
            }
            items(state.projects, key = { it.id }) { project ->
                FeatureEntry(project.title, project.summary, { onIntent(BuildsIntent.OpenProject(project.id)) },
                    icon = DesignR.drawable.icon_builds, status = project.workflowStatus?.let { workspaceStatusLabel(it) } ?: project.statusLabel)
            }
            if (state.projects.isNotEmpty() && state.status == FeatureContentStatus.ERROR) item("cached-error") {
                FeatureNotice(stringResource(R.string.build_error), state.failure?.let { workspaceFailureMessage(it) } ?: stringResource(R.string.build_error_message),
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
    val canChange = !state.isMutating && !state.isRefreshing && state.caseStatus?.isGenerating != true
    var showGenerationOptions by rememberSaveable(state.projectId) { mutableStateOf(false) }
    BackHandler(enabled = state.documentDirty || state.isSaving) { onIntent(BuildIntent.Back) }
    if (state.confirmLeave) AlertDialog(
        onDismissRequest = { if (!state.isSaving) onIntent(BuildIntent.KeepEditing) },
        title = { Text(stringResource(R.string.build_unsaved_title)) },
        text = { Text(stringResource(R.string.build_unsaved_message)) },
        confirmButton = { TextButton({ onIntent(BuildIntent.SaveAndLeave) }, enabled = !state.isSaving) {
            Text(stringResource(R.string.build_save_continue))
        } },
        dismissButton = { FlowRow(horizontalArrangement = Arrangement.End) {
            TextButton({ onIntent(BuildIntent.KeepEditing) }, enabled = !state.isSaving) { Text(stringResource(R.string.build_keep_editing)) }
            TextButton({ onIntent(BuildIntent.DiscardAndLeave) }, enabled = !state.isSaving) { Text(stringResource(R.string.build_discard)) }
        } },
    )
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
        refreshing = state.isRefreshing,
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).imePadding(), contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)) {
            if (state.failure != null || state.status == FeatureContentStatus.ERROR) item("error") {
                FeatureNotice(stringResource(R.string.build_error), state.failure?.let { workspaceFailureMessage(it) } ?: stringResource(R.string.build_error_message),
                    { onIntent(BuildIntent.Reload) })
            }
            if (state.status == FeatureContentStatus.LOADING) item("loading") {
                FeatureLoading(state.progressLabel.ifEmpty { stringResource(R.string.build_generating) })
            }
            if (!state.safeError.isNullOrBlank()) item("generation-error") {
                FeatureNotice(stringResource(R.string.build_generation_issue), state.safeError)
            }
            when (panel) {
                BuildPanel.OVERVIEW -> {
                    val workspace = state.workspace
                    if (workspace == null && state.status != FeatureContentStatus.LOADING) item("empty") {
                        FeatureSection(stringResource(R.string.build_start), stringResource(R.string.build_start_message))
                    }
                    if (workspace == null) item("generation-options") {
                        BuildGenerationForm(state, onIntent, canChange)
                    }
                    if (workspace != null) {
                        item("hero") { FeatureHero(stringResource(R.string.build_scope_eyebrow), workspace.title,
                            workspace.goal, icon = DesignR.drawable.icon_builds) {
                            Text(if (workspace.version > 0) stringResource(R.string.build_version, workspace.version) else workspace.versionLabel,
                                style = MaterialTheme.typography.bodySmall)
                            Text(if (workspace.version > 0) stringResource(if (workspace.approved) R.string.build_approved else R.string.build_draft)
                                else workspace.approvalLabel, style = MaterialTheme.typography.labelLarge)
                            FeatureAction(stringResource(if (workspace.approved) R.string.build_approved else R.string.build_approve),
                                { onIntent(BuildIntent.Approve) }, icon = DesignR.drawable.icon_check, prominent = true,
                                enabled = canChange && !workspace.approved)
                        } }
                        item("users-title") { FeatureSection(stringResource(R.string.build_users)) }
                        items(workspace.users) { FeatureBullet(it) }
                        item("flow-title") { FeatureSection(stringResource(R.string.build_flow)) }
                        items(workspace.primaryFlow) { FeatureBullet(it) }
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
                        workspace.clusterId?.let { clusterId -> item("opportunity") { FeatureEntry(stringResource(R.string.build_view_opportunity),
                            stringResource(R.string.build_view_opportunity_subtitle),
                            { onIntent(BuildIntent.OpenOpportunity(clusterId)) },
                            icon = DesignR.drawable.icon_arrow_up_right) } }
                        item("export") { FeatureAction(stringResource(R.string.build_export),
                            { onIntent(BuildIntent.Export) }, Modifier.fillMaxWidth(), DesignR.drawable.icon_arrow_up_right,
                            enabled = canChange, pending = state.isExporting) }
                        item("regenerate") { OutlinedButton({ showGenerationOptions = !showGenerationOptions },
                            Modifier.fillMaxWidth().heightIn(min = 48.dp), enabled = canChange) {
                            Text(stringResource(R.string.build_generation_options))
                        } }
                        if (showGenerationOptions) item("regeneration-options") { BuildGenerationForm(state, onIntent, canChange) }
                    }
                }
                BuildPanel.FEATURE -> {
                    val feature = state.selectedFeature
                    if (feature == null) item { BuildSelectionMissing { onIntent(BuildIntent.Reload) } }
                    else {
                        item { FeaturePageHeader(stringResource(R.string.build_feature_eyebrow), feature.title, feature.summary) }
                        item { FeatureSection(stringResource(R.string.build_acceptance)) }
                        items(feature.acceptanceCriteria) { FeatureBullet(it) }
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
                            if (state.workspace?.approved != true) Text(stringResource(R.string.build_task_requires_approval),
                                style = MaterialTheme.typography.bodyMedium)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                BuildTaskStatus.entries.forEach { status ->
                                    FilterChip(task.status == status, { onIntent(BuildIntent.TaskStatusSelected(status)) },
                                        label = { Text(taskStatusLabel(status)) }, enabled = canChange && state.workspace?.approved == true)
                                }
                            }
                        }
                        if (state.pendingTaskStatus == BuildTaskStatus.BLOCKED || state.pendingTaskStatus == BuildTaskStatus.DONE)
                            item("task-update") { BuildTaskUpdateForm(state, onIntent, canChange) }
                        item { FeatureSection(stringResource(R.string.build_acceptance)) }
                        items(task.acceptanceChecks) { FeatureBullet(it) }
                        if (task.dependencies.isNotEmpty()) item { FeatureSection(stringResource(R.string.build_dependencies)) }
                        items(task.dependencies) { FeatureBullet(it) }
                    }
                }
                BuildPanel.PLAN -> {
                    item { FeaturePageHeader(stringResource(R.string.build_delivery_eyebrow),
                        stringResource(R.string.build_delivery_title), stringResource(R.string.build_plan_subtitle)) }
                    val workspace = state.workspace
                    if (workspace == null) item { BuildSelectionMissing { onIntent(BuildIntent.Reload) } }
                    else {
                        item { FeatureSection(stringResource(R.string.build_constraints)) }
                        items(workspace.constraints) { FeatureBullet(it) }
                        item { FeatureSection(stringResource(R.string.build_non_goals)) }
                        items(workspace.nonGoals) { FeatureBullet(it) }
                        items(workspace.plan, key = { it.id }) { section ->
                            FeaturePanel {
                                FeatureSection(section.kind?.let { planTitle(it) } ?: section.title)
                                if (section.body.isNotBlank()) Text(section.body, style = MaterialTheme.typography.bodyMedium)
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
                        { onIntent(BuildIntent.Export) }, Modifier.fillMaxWidth(), DesignR.drawable.icon_arrow_up_right,
                        enabled = canChange && state.workspace != null, pending = state.isExporting) }
                }
                BuildPanel.DOCUMENT -> {
                    val document = state.selectedDocument
                    if (document == null) item {
                        BuildSelectionMissing { onIntent(BuildIntent.Reload) }
                        if (state.documentDirty) SelectionContainer { Text(state.documentDraft) }
                    }
                    else {
                        item { FeaturePageHeader(stringResource(R.string.build_document_eyebrow), document.title, document.filename) }
                        if (state.documentDirty) item("draft-status") {
                            Text(stringResource(R.string.build_local_draft), style = MaterialTheme.typography.labelMedium)
                            if (state.documentBaseVersion != state.workspace?.version)
                                Text(stringResource(R.string.build_draft_version_changed), color = MaterialTheme.colorScheme.error)
                            TextButton({ onIntent(BuildIntent.DiscardDocumentChanges) }, enabled = !state.isSaving) {
                                Text(stringResource(R.string.build_use_saved))
                            }
                        }
                        item {
                            if (document.editable) OutlinedTextField(
                                state.documentDraft, { onIntent(BuildIntent.DocumentChanged(it)) },
                                Modifier.fillMaxWidth(), minLines = 12, textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                                label = { Text(stringResource(R.string.build_document_content)) },
                                enabled = !state.isSaving,
                            ) else SelectionContainer {
                                Text(document.content, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace))
                            }
                        }
                        if (document.editable) item { Text(stringResource(R.string.build_save_version_effect),
                            style = MaterialTheme.typography.bodySmall) }
                        if (document.editable) item { FeatureAction(stringResource(R.string.build_save),
                            { onIntent(BuildIntent.SaveDocument) }, Modifier.fillMaxWidth(), DesignR.drawable.icon_check,
                            pending = state.isSaving, enabled = canChange && state.documentDirty) }
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
