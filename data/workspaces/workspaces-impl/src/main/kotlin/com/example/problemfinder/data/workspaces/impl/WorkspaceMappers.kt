package com.example.problemfinder.data.workspaces.impl

import com.example.problemfinder.core.model.details.OpportunityEvidence
import com.example.problemfinder.core.model.workspace.*
import kotlin.time.Instant

private fun String.text(): String = trim().also { require(it.isNotEmpty()) }
internal fun status(value: String): WorkspaceStatus = WorkspaceStatus.valueOf(value.uppercase())
private fun List<String>.identities() { require(all { it.isNotBlank() }); require(distinct().size == size) }
internal fun ProjectDto.toModel(owner: String): WorkflowProject {
    require(userId == owner)
    val source = sourceClusterId?.let { requireNotNull(it.toIntOrNull()).also { id -> require(id > 0) } }
    return WorkflowProject(id.text(), userId, (displayName ?: name).text(), source, stage.text(),
        status(analysisStatus), status(buildStatus), approvalStatus.text(), launchedAt != null)
}
internal fun AnalysisCacheDto.toModel(owner: String): AnalysisWorkspaceData {
    val parent = project.toModel(owner)
    val result = analysis?.let { document ->
        require(document.project.pipelineId == parent.id && document.version >= 0)
        require(!status(document.status).let { it == WorkspaceStatus.READY || it == WorkspaceStatus.STALE } || document.version > 0)
        document.problemUnits.map { it.id }.identities()
        document.claims.map { it.id }.identities()
        document.evidence.map { it.id }.identities()
        document.audiences.map { it.id }.identities()
        document.assumptions.map { it.id }.identities()
        val evidenceIds = document.evidence.map { it.id }.toSet()
        require(document.problemUnits.all { evidenceIds.containsAll(it.evidenceIds) })
        require(document.claims.all { it.kind in setOf("observed", "inferred", "user_confirmed") &&
            it.confidence in setOf("low", "medium", "high") && evidenceIds.containsAll(it.evidenceIds) })
        require(document.problemUnits.all { it.kind in setOf("cause", "core_problem", "symptom", "consequence", "workaround") })
        if (document.status == "ready") require(document.evidence.isNotEmpty() && (document.problemUnits.isNotEmpty() || document.claims.isNotEmpty()))
        AnalysisCase(parent.id, document.version, status(document.status), document.progress?.label, document.safeError,
            document.thesis?.let { AnalysisThesis(it.statement.text(), it.consequence, it.workaround, it.confirmed) },
            document.claims.map { AnalysisClaim(it.id.text(), it.text.text(), it.kind, it.confidence, it.evidenceIds, it.confirmed, it.rejected) },
            document.problemUnits.map { ProblemUnit(it.id.text(), it.title.text(), it.description, it.kind,
                it.affectedUsers, it.situation, it.evidenceIds, it.pinned, it.rejected) },
            document.audiences.map { Audience(it.id.text(), it.name.text(), it.description.text(), it.language, it.communities, it.reachChannels, it.unknowns) },
            document.assumptions.map { Assumption(it.id.text(), it.question.text(), it.whyItMatters.text(), it.resolutionEvidence.text(), it.resolved) },
            document.evidence.map { OpportunityEvidence(
                it.id.text(), it.title.text(), it.body ?: it.excerpt,
                it.sourceUrl, it.author, it.community, it.platform, it.score, it.commentCount,
                it.observedAt?.let(Instant::parse), null,
            ) })
    }
    return AnalysisWorkspaceData(parent, result)
}
internal fun ConversationDto.toModel(): AnalysisConversation {
    turns.map { it.id }.identities()
    return AnalysisConversation(id.text(), title.text(), turns.map { turn ->
        require(turn.role == "user" || turn.role == "assistant")
        ConversationTurn(turn.id.text(), turn.text.text(), turn.role == "user",
            turn.citations.map { AnalysisCitation(it.evidenceId.text(), it.label.text()) },
            turn.proposal?.let { AnalysisProposal(it.id.text(), it.title.text(), it.summary.text(), it.status) },
            turn.insufficientEvidence)
    })
}
internal fun BuildCacheDto.toModel(owner: String): BuildWorkspaceData {
    val parent = project.toModel(owner)
    val document = solution
    require(document.project.pipelineId == parent.id && document.version >= 0)
    require(document.status != "ready" || (document.version > 0 && !document.mvpGoal.isNullOrBlank()))
    require(document.approvalStatus in setOf("draft", "approved"))
    if (document.status == "ready") require(document.primaryFlow != null && document.architecture != null &&
        document.testPlan != null && document.features.isNotEmpty() && document.tasks.isNotEmpty() && document.nonGoals.isNotEmpty())
    if (document.approvalStatus == "approved") require(document.approvedVersion == document.version && document.version > 0)
    document.features.map { it.id }.identities()
    document.tasks.map { it.id }.identities()
    document.documents.map { it.key }.identities()
    val taskIds = document.tasks.map { it.id }.toSet()
    require(document.tasks.all { taskIds.containsAll(it.dependsOn) })
    return BuildWorkspaceData(parent, SolutionCase(
        parent.id, document.version, status(document.status), document.progress?.label, document.safeError,
        document.approvalStatus, document.approvedVersion,
        document.project.deliveryTargets.map { DeliveryTarget.valueOf(it.uppercase()) }.toSet().also { require(it.isNotEmpty()) },
        document.primaryUsers, document.mvpGoal, document.constraints, document.nonGoals,
        document.primaryFlow?.let { listOf(it.trigger) + it.steps + it.successOutcome }.orEmpty(),
        document.features.map { SolutionFeature(it.id.text(), it.title.text(), it.summary.text(), it.acceptanceCriteria) },
        document.tasks.map { SolutionTask(it.id.text(), it.title.text(), it.description.text(),
            TaskProgress.valueOf(it.status.uppercase()), it.acceptanceChecks, it.dependsOn) },
        document.architecture?.let { a -> SolutionArchitecture(a.stack,
            a.components.map { ArchitectureComponent(it.name.text(), it.responsibility.text()) },
            a.dataModel.map { ArchitectureEntity(it.name.text(), it.purpose.text(), it.fields) },
            a.apiContracts.map { ArchitectureApi(it.method, it.path, it.purpose) },
            a.integrations, a.securityAndPrivacy) },
        document.testPlan?.let { SolutionTestPlan(it.unit, it.integration, it.endToEnd) },
        document.deliveryChecklist.map { SolutionDelivery(it.id.text(), it.label.text(), it.required, it.status) },
        document.documents.map { SolutionDocument(it.key.text(), it.title.text(), it.filename.text(), it.content, it.editable) },
    ))
}
