package com.example.problemfinder.feature.projects.impl


import com.example.problemfinder.core.designsystem.components.FeatureContentStatus
import com.example.problemfinder.core.model.details.OpportunityEvidence
import com.example.problemfinder.core.model.workspace.*

/** Presentation contracts derived from account-scoped project and analysis snapshots. */
enum class AnalysisStage { WATCHING, IN_REVIEW, READY }
enum class AnalysisPanel { OVERVIEW, PROBLEM, AUDIENCE, ASSUMPTIONS, CONVERSATION }
data class AnalysisProjectCard(val id: String, val title: String, val summary: String, val stageLabel: String,
    val workflowStatus: WorkspaceStatus? = null)
data class ProjectsState(
    val projects: List<AnalysisProjectCard> = emptyList(),
    val stage: AnalysisStage = AnalysisStage.WATCHING,
    val status: FeatureContentStatus = FeatureContentStatus.EMPTY,
    val userId: String? = null,
    val cacheReady: Boolean = false,
    val isRefreshing: Boolean = false,
    val failure: WorkspaceFailure? = null,
)
data class AnalysisProblem(
    val id: String, val title: String, val description: String,
    val kindLabel: String = "", val affectedUsers: List<String> = emptyList(),
    val situation: String = "", val evidence: List<OpportunityEvidence> = emptyList(),
    val pinned: Boolean = false, val rejected: Boolean = false,
)
data class AnalysisAudience(
    val id: String, val name: String, val description: String,
    val language: List<String> = emptyList(), val communities: List<String> = emptyList(),
    val reachChannels: List<String> = emptyList(), val unknowns: List<String> = emptyList(),
)
data class AnalysisAssumption(
    val id: String, val question: String, val whyItMatters: String,
    val resolutionEvidence: String, val resolved: Boolean = false,
)
data class AnalysisTurn(val id: String, val text: String, val isUser: Boolean,
    val citations: List<AnalysisCitation> = emptyList(), val proposal: AnalysisProposal? = null,
    val insufficientEvidence: Boolean = false)
data class AnalysisWorkspace(
    val id: String, val clusterId: Int?, val title: String, val summary: String,
    val versionLabel: String = "", val thesis: String = "",
    val consequence: String = "", val workaround: String = "",
    val problems: List<AnalysisProblem> = emptyList(),
    val audiences: List<AnalysisAudience> = emptyList(),
    val assumptions: List<AnalysisAssumption> = emptyList(),
    val thesisConfirmed: Boolean = false,
)
data class AnalysisWorkspaceState(
    val projectId: String? = null,
    val workspace: AnalysisWorkspace? = null,
    val selectedProblem: AnalysisProblem? = null,
    val selectedAudience: AnalysisAudience? = null,
    val status: FeatureContentStatus = FeatureContentStatus.EMPTY,
    val progressLabel: String = "",
    val turns: List<AnalysisTurn> = emptyList(),
    val draftMessage: String = "",
    val isSending: Boolean = false,
    val userId: String? = null,
    val cacheReady: Boolean = false,
    val caseStatus: WorkspaceStatus? = null,
    val version: Int = 0,
    val isRefreshing: Boolean = false,
    val isMutating: Boolean = false,
    val failure: WorkspaceFailure? = null,
    val safeError: String? = null,
    val claims: List<AnalysisClaim> = emptyList(),
    val conversations: List<ConversationSummary> = emptyList(),
    val conversationId: String? = null,
    val isLoadingConversation: Boolean = false,
    val conversationFailure: WorkspaceFailure? = null,
)
sealed interface ProjectsIntent {
    data object Entered : ProjectsIntent
    data object Reload : ProjectsIntent
    data class StageSelected(val stage: AnalysisStage) : ProjectsIntent
    data class OpenProject(val id: String) : ProjectsIntent
    data object Discover : ProjectsIntent
}
sealed interface ProjectsEffect {
    data class OpenProject(val id: String) : ProjectsEffect
    data object Discover : ProjectsEffect
}
sealed interface AnalysisIntent {
    data class VisibilityChanged(val owner: String, val visible: Boolean) : AnalysisIntent
    data class ConversationSelected(val id: String) : AnalysisIntent
    data object NewConversation : AnalysisIntent
    data object RetryConversation : AnalysisIntent
    data class DecideProposal(val id: String, val accept: Boolean) : AnalysisIntent
    data class Entered(val itemId: String? = null, val panel: AnalysisPanel = AnalysisPanel.OVERVIEW) : AnalysisIntent
    data object Back : AnalysisIntent
    data object Reload : AnalysisIntent
    data object Generate : AnalysisIntent
    data object ConfirmThesis : AnalysisIntent
    data class OpenProblem(val id: String) : AnalysisIntent
    data class OpenAudience(val id: String) : AnalysisIntent
    data object OpenAssumptions : AnalysisIntent
    data object OpenConversation : AnalysisIntent
    data class OpenOpportunity(val clusterId: Int) : AnalysisIntent
    data class OpenPost(val clusterId: Int, val postId: Int) : AnalysisIntent
    data object OpenBuild : AnalysisIntent
    data class PinProblem(val id: String) : AnalysisIntent
    data class RejectProblem(val id: String) : AnalysisIntent
    data class ResolveAssumption(val id: String, val resolved: Boolean) : AnalysisIntent
    data class MessageChanged(val value: String) : AnalysisIntent
    data object SendMessage : AnalysisIntent
}
sealed interface AnalysisEffect {
    data object Back : AnalysisEffect
    data class OpenProblem(val id: String) : AnalysisEffect
    data class OpenAudience(val id: String) : AnalysisEffect
    data object OpenAssumptions : AnalysisEffect
    data object OpenConversation : AnalysisEffect
    data class OpenOpportunity(val clusterId: Int) : AnalysisEffect
    data class OpenPost(val clusterId: Int, val postId: Int) : AnalysisEffect
    data object OpenBuild : AnalysisEffect
}
