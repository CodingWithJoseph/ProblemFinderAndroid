package com.example.problemfinder.domain.workspaces.testing

import com.example.problemfinder.core.model.workspace.*
import com.example.problemfinder.core.model.details.OpportunityEvidence
import com.example.problemfinder.domain.workspaces.api.WorkspaceWorkflows
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow

fun fixtureProject(owner: String = "user-a", id: String = "project-1") = WorkflowProject(
    id, owner, "A useful project", 42, "watching", WorkspaceStatus.READY, WorkspaceStatus.NOT_STARTED, "draft", false)
fun fixtureAnalysis(owner: String = "user-a", status: WorkspaceStatus = WorkspaceStatus.READY) = AnalysisWorkspaceData(
    fixtureProject(owner), AnalysisCase("project-1", 1, status, null, null,
        AnalysisThesis("A supported thesis", null, null, false), emptyList(),
        listOf(ProblemUnit("problem-1", "A recurring problem", "The problem", "core_problem",
            listOf("Small teams"), "During a build", listOf("101"), false, false)), emptyList(),
        listOf(Assumption("assumption-1", "Does it recur?", "Frequency matters", "Ask users", false)),
        listOf(OpportunityEvidence("101", "A source post", "A useful report", null, null, null, "GitHub", null, null, null, null))))
fun fixtureBuild(owner: String = "user-a", version: Int = 1, content: String = "Saved document") = BuildWorkspaceData(
    fixtureProject(owner), SolutionCase(
        projectId = "project-1", version = version, status = WorkspaceStatus.READY, progress = null, safeError = null,
        approvalStatus = "approved", approvedVersion = version, targets = setOf(DeliveryTarget.ANDROID),
        users = listOf("Small teams"), goal = "Explain a build failure", constraints = emptyList(), nonGoals = listOf("Rewrite code"),
        primaryFlow = listOf("Inspect the error"), features = listOf(SolutionFeature("feature-1", "Explain failure", "Identify the cause", listOf("Show the cause"))),
        tasks = listOf(SolutionTask("task-1", "Reproduce failure", "Create a reproduction", TaskProgress.TODO, listOf("It fails consistently"), emptyList())),
        architecture = SolutionArchitecture(listOf("Kotlin"), listOf(ArchitectureComponent("App", "Show an explanation")), emptyList(), emptyList(), emptyList(), emptyList()),
        testPlan = SolutionTestPlan(emptyList(), emptyList(), listOf("Open a failure and see the explanation")), delivery = emptyList(),
        documents = listOf(SolutionDocument("requirements", "Requirements", "REQUIREMENTS.md", content, true))))
data class DocumentSaveCall(val projectId: String, val key: String, val version: Int, val content: String)
data class TaskUpdateCall(val projectId: String, val taskId: String, val version: Int, val update: TaskUpdate)

/** Test boundary only; no production sample data. */
open class FakeWorkspaceWorkflows : WorkspaceWorkflows {
    val projects = MutableStateFlow(WorkspaceSnapshot("user-a", listOf(fixtureProject()), true))
    val analysis = MutableStateFlow(WorkspaceSnapshot("user-a", fixtureAnalysis(), true))
    val build = MutableStateFlow(WorkspaceSnapshot("user-a", fixtureBuild(), true))
    val conversationList = MutableStateFlow(WorkspaceSnapshot("user-a", emptyList<ConversationSummary>(), true))
    val conversation = MutableStateFlow(WorkspaceSnapshot("user-a", AnalysisConversation("conversation-1", "Conversation", emptyList()), true))
    val events = mutableListOf<String>()
    val drafts = mutableMapOf<Triple<String, String, String>, DocumentDraft>()
    val taskUpdates = mutableListOf<TaskUpdateCall>()
    val documentSaves = mutableListOf<DocumentSaveCall>()
    var onRefreshProjects: suspend () -> Unit = {}
    var onRefreshAnalysis: suspend () -> Unit = {}
    var onGenerateAnalysis: suspend () -> Unit = {}
    var onRefreshBuild: suspend () -> Unit = {}
    var onGenerateBuild: suspend (BuildGeneration) -> Unit = {}
    var onSaveDocument: (suspend (DocumentSaveCall) -> SolutionCase)? = null
    var onReadDraft: (suspend () -> Unit)? = null
    var onSend: suspend (String) -> Unit = {}
    override fun observeProjects() = projects
    override fun observeAnalysis(projectId: String): Flow<WorkspaceSnapshot<AnalysisWorkspaceData>> = analysis
    override fun observeBuild(projectId: String) = build
    override fun observeConversationList(projectId: String) = conversationList
    override fun observeConversation(projectId: String, conversationId: String) = conversation
    override suspend fun refreshProjects() { events += "projects.refresh"; onRefreshProjects() }
    override suspend fun refreshAnalysis(projectId: String) { events += "analysis.refresh"; onRefreshAnalysis() }
    override suspend fun generateAnalysis(projectId: String) { events += "analysis.generate"; onGenerateAnalysis() }
    override suspend fun confirmThesis(projectId: String) { events += "analysis.confirm" }
    override suspend fun pinProblem(projectId: String, problemId: String, pinned: Boolean) { events += "problem.pin:" + pinned }
    override suspend fun rejectProblem(projectId: String, problemId: String, rejected: Boolean) { events += "problem.reject:" + rejected }
    override suspend fun resolveAssumption(projectId: String, assumptionId: String, resolved: Boolean) { events += "assumption:" + resolved }
    override suspend fun listConversations(projectId: String) = conversationList.value.value.orEmpty()
    override suspend fun createConversation(projectId: String): AnalysisConversation {
        events += "conversation.create"; return requireNotNull(conversation.value.value)
    }
    override suspend fun loadConversation(projectId: String, conversationId: String) = requireNotNull(conversation.value.value)
    override suspend fun sendMessage(projectId: String, conversationId: String?, message: String): AnalysisConversation {
        events += "conversation.send"; onSend(message); return requireNotNull(conversation.value.value)
    }
    override suspend fun decideProposal(projectId: String, conversationId: String, proposalId: String, accept: Boolean): AnalysisConversation {
        events += "proposal:" + accept; return requireNotNull(conversation.value.value)
    }
    override suspend fun refreshBuild(projectId: String) { events += "build.refresh"; onRefreshBuild() }
    override suspend fun generateBuild(projectId: String, request: BuildGeneration) { events += "build.generate"; onGenerateBuild(request) }
    override suspend fun approveBuild(projectId: String, expectedVersion: Int) { events += "build.approve:" + expectedVersion }
    override suspend fun updateTask(projectId: String, taskId: String, expectedVersion: Int, update: TaskUpdate) {
        taskUpdates += TaskUpdateCall(projectId, taskId, expectedVersion, update)
    }
    override suspend fun saveDocument(projectId: String, key: String, expectedVersion: Int, content: String): SolutionCase {
        val call = DocumentSaveCall(projectId, key, expectedVersion, content); documentSaves += call
        return onSaveDocument?.invoke(call) ?: requireNotNull(build.value.value).solution.copy(version = expectedVersion + 1,
            approvalStatus = "draft", approvedVersion = null,
            documents = requireNotNull(build.value.value).solution.documents.map { if (it.key == key) it.copy(content = content) else it })
    }
    override suspend fun readDocumentDraft(projectId: String, key: String): DocumentDraft? {
        onReadDraft?.invoke(); return drafts[Triple(build.value.userId.orEmpty(), projectId, key)]
    }
    override suspend fun saveDocumentDraft(userId: String, projectId: String, draft: DocumentDraft) { drafts[Triple(userId, projectId, draft.key)] = draft }
    override suspend fun clearDocumentDraft(userId: String, projectId: String, key: String) { drafts.remove(Triple(userId, projectId, key)) }
    override suspend fun exportBuild(projectId: String): WorkspaceExport {
        events += "build.export"; return WorkspaceExport("build.zip", byteArrayOf(1, 2, 3))
    }
}
