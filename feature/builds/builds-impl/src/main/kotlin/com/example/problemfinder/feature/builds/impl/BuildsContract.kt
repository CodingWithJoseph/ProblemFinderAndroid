package com.example.problemfinder.feature.builds.impl


import com.example.problemfinder.core.designsystem.components.FeatureContentStatus

enum class BuildPanel { OVERVIEW, FEATURE, TASK, PLAN, DOCUMENTS, DOCUMENT }
enum class BuildTaskStatus { TODO, IN_PROGRESS, BLOCKED, DONE }
data class BuildProjectCard(val id: String, val title: String, val summary: String, val statusLabel: String)
data class BuildsState(
    val projects: List<BuildProjectCard> = emptyList(),
    val status: FeatureContentStatus = FeatureContentStatus.EMPTY,
)
data class BuildFeature(
    val id: String, val title: String, val summary: String,
    val acceptanceCriteria: List<String> = emptyList(),
)
data class BuildTask(
    val id: String, val title: String, val description: String,
    val status: BuildTaskStatus = BuildTaskStatus.TODO,
    val acceptanceChecks: List<String> = emptyList(),
    val dependencies: List<String> = emptyList(),
)
data class BuildPlanSection(val id: String, val title: String, val body: String, val items: List<String> = emptyList())
data class BuildDocument(val id: String, val title: String, val filename: String, val content: String, val editable: Boolean = false)
data class BuildWorkspace(
    val id: String, val clusterId: Int, val title: String, val goal: String,
    val versionLabel: String = "", val approvalLabel: String = "",
    val users: List<String> = emptyList(), val constraints: List<String> = emptyList(),
    val nonGoals: List<String> = emptyList(), val primaryFlow: List<String> = emptyList(),
    val features: List<BuildFeature> = emptyList(), val tasks: List<BuildTask> = emptyList(),
    val plan: List<BuildPlanSection> = emptyList(), val documents: List<BuildDocument> = emptyList(),
)
data class BuildWorkspaceState(
    val projectId: String? = null,
    val workspace: BuildWorkspace? = null,
    val selectedFeature: BuildFeature? = null,
    val selectedTask: BuildTask? = null,
    val selectedDocument: BuildDocument? = null,
    val documentDraft: String = "",
    val status: FeatureContentStatus = FeatureContentStatus.EMPTY,
    val progressLabel: String = "",
    val isSaving: Boolean = false,
)
sealed interface BuildsIntent {
    data object Entered : BuildsIntent
    data object Reload : BuildsIntent
    data class OpenProject(val id: String) : BuildsIntent
    data object OpenAnalyze : BuildsIntent
}
sealed interface BuildsEffect {
    data class OpenProject(val id: String) : BuildsEffect
    data object OpenAnalyze : BuildsEffect
}
sealed interface BuildIntent {
    data class Entered(val itemId: String? = null, val panel: BuildPanel = BuildPanel.OVERVIEW) : BuildIntent
    data object Back : BuildIntent
    data object Reload : BuildIntent
    data object Generate : BuildIntent
    data object Approve : BuildIntent
    data class OpenFeature(val id: String) : BuildIntent
    data class OpenTask(val id: String) : BuildIntent
    data object OpenPlan : BuildIntent
    data object OpenDocuments : BuildIntent
    data class OpenDocument(val id: String) : BuildIntent
    data object OpenAnalysis : BuildIntent
    data class OpenOpportunity(val clusterId: Int) : BuildIntent
    data class TaskStatusSelected(val status: BuildTaskStatus) : BuildIntent
    data class DocumentChanged(val value: String) : BuildIntent
    data object SaveDocument : BuildIntent
    data object Export : BuildIntent
}
sealed interface BuildEffect {
    data object Back : BuildEffect
    data class OpenFeature(val id: String) : BuildEffect
    data class OpenTask(val id: String) : BuildEffect
    data object OpenPlan : BuildEffect
    data object OpenDocuments : BuildEffect
    data class OpenDocument(val id: String) : BuildEffect
    data object OpenAnalysis : BuildEffect
    data class OpenOpportunity(val clusterId: Int) : BuildEffect
}
