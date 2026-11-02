package com.example.problemfinder.core.model.workspace

import com.example.problemfinder.core.model.details.OpportunityEvidence

/** Account-owned workflow documents. JSON/Room details stay in the data implementation. */
enum class WorkspaceStatus { NOT_STARTED, QUEUED, GENERATING, READY, STALE, INSUFFICIENT_EVIDENCE, FAILED;
    val isGenerating get() = this == QUEUED || this == GENERATING
}
enum class WorkspaceFailure { SESSION, NETWORK, NOT_FOUND, CONFLICT, REJECTED, SERVER, INVALID_RESPONSE, UNKNOWN }
class WorkspaceException(val failure: WorkspaceFailure, cause: Throwable? = null) : Exception(cause)

data class WorkspaceSnapshot<T>(val userId: String?, val value: T? = null, val cacheReady: Boolean = false,
    val cacheFailure: WorkspaceFailure? = null)

data class WorkflowProject(
    val id: String, val userId: String, val name: String, val clusterId: Int?,
    val stage: String, val analysisStatus: WorkspaceStatus, val buildStatus: WorkspaceStatus,
    val approvalStatus: String, val launched: Boolean,
) {
    init { require(id.isNotBlank() && userId.isNotBlank() && name.isNotBlank()); require(clusterId == null || clusterId > 0) }
}

data class AnalysisThesis(val statement: String, val consequence: String?, val workaround: String?, val confirmed: Boolean)
data class AnalysisClaim(
    val id: String, val text: String, val kind: String, val confidence: String,
    val evidenceIds: List<String>, val confirmed: Boolean, val rejected: Boolean,
)
data class ProblemUnit(
    val id: String, val title: String, val description: String?, val kind: String,
    val affectedUsers: List<String>, val situation: String?, val evidenceIds: List<String>,
    val pinned: Boolean, val rejected: Boolean,
)
data class Audience(
    val id: String, val name: String, val description: String, val language: List<String>,
    val communities: List<String>, val reachChannels: List<String>, val unknowns: List<String>,
)
data class Assumption(
    val id: String, val question: String, val whyItMatters: String, val resolutionEvidence: String, val resolved: Boolean,
)
data class AnalysisCase(
    val projectId: String, val version: Int, val status: WorkspaceStatus,
    val progress: String?, val safeError: String?, val thesis: AnalysisThesis?,
    val claims: List<AnalysisClaim>, val problems: List<ProblemUnit>, val audiences: List<Audience>,
    val assumptions: List<Assumption>, val evidence: List<OpportunityEvidence>,
)
data class AnalysisWorkspaceData(val project: WorkflowProject, val analysis: AnalysisCase?)

data class ConversationSummary(val id: String, val title: String, val archived: Boolean)
data class AnalysisCitation(val evidenceId: Int, val label: String)
data class AnalysisProposal(val id: String, val title: String, val summary: String, val status: String)
data class ConversationTurn(
    val id: String, val text: String, val isUser: Boolean, val citations: List<AnalysisCitation>,
    val proposal: AnalysisProposal?, val insufficientEvidence: Boolean,
)
data class AnalysisConversation(val id: String, val title: String, val turns: List<ConversationTurn>)

enum class DeliveryTarget { WEB, IOS, ANDROID }
enum class TaskProgress { TODO, IN_PROGRESS, BLOCKED, DONE }
data class BuildGeneration(
    val targets: Set<DeliveryTarget> = setOf(DeliveryTarget.WEB),
    val constraints: List<String> = emptyList(), val preferredStack: List<String> = emptyList(),
) {
    init { require(targets.isNotEmpty()); require(constraints.size <= 12 && preferredStack.size <= 12) }
}
data class SolutionFeature(val id: String, val title: String, val summary: String, val acceptanceCriteria: List<String>)
data class SolutionTask(
    val id: String, val title: String, val description: String, val status: TaskProgress,
    val acceptanceChecks: List<String>, val dependencies: List<String>,
)
data class ArchitectureComponent(val name: String, val responsibility: String)
data class ArchitectureEntity(val name: String, val purpose: String, val fields: List<String>)
data class ArchitectureApi(val method: String, val path: String, val purpose: String)
data class SolutionArchitecture(
    val stack: List<String>, val components: List<ArchitectureComponent>, val entities: List<ArchitectureEntity>,
    val apis: List<ArchitectureApi>, val integrations: List<String>, val security: List<String>,
)
data class SolutionTestPlan(val unit: List<String>, val integration: List<String>, val endToEnd: List<String>)
data class SolutionDelivery(val id: String, val label: String, val required: Boolean, val status: String)
data class SolutionDocument(val key: String, val title: String, val filename: String, val content: String, val editable: Boolean)
data class SolutionCase(
    val projectId: String, val version: Int, val status: WorkspaceStatus, val progress: String?, val safeError: String?,
    val approvalStatus: String, val approvedVersion: Int?, val targets: Set<DeliveryTarget>,
    val users: List<String>, val goal: String?, val constraints: List<String>, val nonGoals: List<String>,
    val primaryFlow: List<String>, val features: List<SolutionFeature>, val tasks: List<SolutionTask>,
    val architecture: SolutionArchitecture?, val testPlan: SolutionTestPlan?, val delivery: List<SolutionDelivery>,
    val documents: List<SolutionDocument>,
)
data class BuildWorkspaceData(val project: WorkflowProject, val solution: SolutionCase)
/** The server checks every supplied verification result before allowing Done. */
data class TaskCheck(val command: String, val details: String? = null)
data class TaskUpdate(
    val status: TaskProgress, val note: String? = null, val verificationSummary: String? = null,
    val checks: List<TaskCheck> = emptyList(),
) {
    init {
        require(status != TaskProgress.BLOCKED || !note.isNullOrBlank())
        require(status != TaskProgress.DONE || (!verificationSummary.isNullOrBlank() && checks.isNotEmpty()))
        require(checks.all { it.command.isNotBlank() && it.command.length <= 500 })
        require(note == null || note.length <= 2000)
        require(verificationSummary == null || verificationSummary.length <= 2000)
        require(checks.size <= 20)
    }
}
data class WorkspaceExport(val filename: String, val content: ByteArray)
data class DocumentDraft(val key: String, val baseVersion: Int, val content: String)
