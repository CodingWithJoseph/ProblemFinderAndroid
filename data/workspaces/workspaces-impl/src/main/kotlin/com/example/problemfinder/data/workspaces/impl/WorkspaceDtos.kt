package com.example.problemfinder.data.workspaces.impl

import kotlinx.serialization.Serializable

@Serializable internal data class ProjectsDto(val data: List<ProjectDto>)
@Serializable internal data class ProjectEnvelopeDto(val data: ProjectDto)
@Serializable internal data class ProjectDto(
    val id: String, val userId: String, val name: String, val displayName: String? = null,
    val sourceClusterId: String? = null, val stage: String, val analysisStatus: String,
    val buildStatus: String, val approvalStatus: String, val launchedAt: String? = null,
)
@Serializable internal data class ContextDto(val pipelineId: String, val projectName: String)
@Serializable internal data class ProgressDto(val label: String)
@Serializable internal data class ThesisDto(
    val statement: String, val consequence: String? = null, val workaround: String? = null, val confirmed: Boolean,
)
@Serializable internal data class ClaimDto(
    val id: String, val text: String, val kind: String, val confidence: String,
    val evidenceIds: List<String>, val confirmed: Boolean, val rejected: Boolean,
)
@Serializable internal data class ProblemDto(
    val id: String, val title: String, val description: String? = null, val kind: String,
    val affectedUsers: List<String> = emptyList(), val situation: String? = null,
    val evidenceIds: List<String>, val pinned: Boolean, val rejected: Boolean,
)
@Serializable internal data class AudienceDto(
    val id: String, val name: String, val description: String,
    val language: List<String>, val communities: List<String>, val reachChannels: List<String>, val unknowns: List<String>,
)
@Serializable internal data class AssumptionDto(
    val id: String, val question: String, val whyItMatters: String, val resolutionEvidence: String, val resolved: Boolean,
)
@Serializable internal data class EvidenceDto(
    val id: String, val title: String, val excerpt: String, val body: String? = null,
    val platform: String? = null, val community: String? = null, val author: String? = null,
    val observedAt: String? = null, val score: Int? = null, val commentCount: Int? = null,
    val sourceUrl: String? = null,
)
@Serializable internal data class AnalysisDto(
    val version: Int, val status: String, val project: ContextDto,
    val progress: ProgressDto? = null, val safeError: String? = null, val thesis: ThesisDto? = null,
    val claims: List<ClaimDto>, val problemUnits: List<ProblemDto>, val audiences: List<AudienceDto>,
    val assumptions: List<AssumptionDto>, val evidence: List<EvidenceDto>,
)
@Serializable internal data class AnalysisCacheDto(val project: ProjectDto, val analysis: AnalysisDto?)
@Serializable internal data class ConversationSummaryDto(val id: String, val title: String, val archived: Boolean)
@Serializable internal data class CitationDto(val evidenceId: String, val label: String)
@Serializable internal data class ProposalDto(val id: String, val title: String, val summary: String, val status: String)
@Serializable internal data class TurnDto(
    val id: String, val role: String, val text: String, val citations: List<CitationDto>,
    val proposal: ProposalDto? = null, val insufficientEvidence: Boolean,
)
@Serializable internal data class ConversationDto(val id: String, val title: String, val turns: List<TurnDto>)
@Serializable internal data class FeatureDto(val id: String, val title: String, val summary: String, val acceptanceCriteria: List<String>)
@Serializable internal data class TaskDto(
    val id: String, val title: String, val description: String, val status: String,
    val acceptanceChecks: List<String>, val dependsOn: List<String>,
)
@Serializable internal data class ComponentDto(val name: String, val responsibility: String)
@Serializable internal data class DataEntityDto(val name: String, val purpose: String, val fields: List<String>)
@Serializable internal data class ApiDto(val method: String, val path: String, val purpose: String)
@Serializable internal data class ArchitectureDto(
    val stack: List<String>, val components: List<ComponentDto>, val dataModel: List<DataEntityDto>,
    val apiContracts: List<ApiDto>, val integrations: List<String>, val securityAndPrivacy: List<String>,
)
@Serializable internal data class TestPlanDto(val unit: List<String>, val integration: List<String>, val endToEnd: List<String>)
@Serializable internal data class DeliveryDto(val id: String, val label: String, val required: Boolean, val status: String)
@Serializable internal data class DocumentDto(val key: String, val title: String, val filename: String, val content: String, val editable: Boolean)
@Serializable internal data class SolutionContextDto(
    val pipelineId: String, val projectName: String, val deliveryTargets: List<String>,
)
@Serializable internal data class PrimaryFlowDto(val actor: String, val trigger: String, val steps: List<String>, val successOutcome: String)
@Serializable internal data class SolutionDto(
    val version: Int, val status: String, val project: SolutionContextDto,
    val progress: ProgressDto? = null, val safeError: String? = null,
    val approvalStatus: String, val approvedVersion: Int? = null,
    val primaryUsers: List<String>, val mvpGoal: String? = null,
    val constraints: List<String>, val nonGoals: List<String>, val primaryFlow: PrimaryFlowDto? = null,
    val features: List<FeatureDto>, val tasks: List<TaskDto>, val architecture: ArchitectureDto? = null,
    val testPlan: TestPlanDto? = null, val deliveryChecklist: List<DeliveryDto>, val documents: List<DocumentDto>,
)
@Serializable internal data class BuildCacheDto(val project: ProjectDto, val solution: SolutionDto)
@Serializable internal data class GenerationDto(val deliveryTargets: List<String>, val constraints: List<String>, val preferredStack: List<String>)
@Serializable internal data class VersionDto(val expectedVersion: Int)
@Serializable internal data class DraftDto(val key: String, val baseVersion: Int, val content: String)
@Serializable internal data class DocumentUpdateDto(val expectedVersion: Int, val content: String)
@Serializable internal data class MessageDto(val message: String)
@Serializable internal data class ConversationCreateDto(val title: String? = null)
@Serializable internal data class DecisionDto(val status: String)
@Serializable internal data class VerificationCheckDto(val command: String, val outcome: String, val details: String? = null)
@Serializable internal data class VerificationDto(val summary: String, val checks: List<VerificationCheckDto>)
@Serializable internal data class TaskUpdateDto(
    val expectedVersion: Int, val status: String, val note: String? = null, val verification: VerificationDto? = null,
)
@Serializable internal data class TaskUpdateResultDto(val taskId: String, val status: String)
