package com.example.problemfinder.data.workspaces.api

import com.example.problemfinder.core.model.workspace.*
import kotlinx.coroutines.flow.Flow

interface WorkspaceRepository {
    fun observeProjects(): Flow<WorkspaceSnapshot<List<WorkflowProject>>>
    fun observeAnalysis(projectId: String): Flow<WorkspaceSnapshot<AnalysisWorkspaceData>>
    fun observeBuild(projectId: String): Flow<WorkspaceSnapshot<BuildWorkspaceData>>
    suspend fun refreshProjects()
    suspend fun refreshAnalysis(projectId: String)
    suspend fun generateAnalysis(projectId: String)
    suspend fun updateAnalysis(projectId: String, kind: String, objectId: String, property: String, value: Boolean)
    suspend fun listConversations(projectId: String): List<ConversationSummary>
    fun observeConversationList(projectId: String): Flow<WorkspaceSnapshot<List<ConversationSummary>>>
    fun observeConversation(projectId: String, conversationId: String): Flow<WorkspaceSnapshot<AnalysisConversation>>
    suspend fun createConversation(projectId: String): AnalysisConversation
    suspend fun loadConversation(projectId: String, conversationId: String): AnalysisConversation
    suspend fun sendMessage(projectId: String, conversationId: String?, message: String): AnalysisConversation
    suspend fun decideProposal(projectId: String, conversationId: String, proposalId: String, accept: Boolean): AnalysisConversation
    suspend fun refreshBuild(projectId: String)
    suspend fun generateBuild(projectId: String, request: BuildGeneration)
    suspend fun approveBuild(projectId: String, expectedVersion: Int)
    suspend fun updateTask(projectId: String, taskId: String, expectedVersion: Int, update: TaskUpdate)
    suspend fun saveDocument(projectId: String, key: String, expectedVersion: Int, content: String): SolutionCase
    suspend fun readDocumentDraft(projectId: String, key: String): DocumentDraft?
    suspend fun saveDocumentDraft(userId: String, projectId: String, draft: DocumentDraft)
    suspend fun clearDocumentDraft(userId: String, projectId: String, key: String)
    suspend fun exportBuild(projectId: String): WorkspaceExport
}
