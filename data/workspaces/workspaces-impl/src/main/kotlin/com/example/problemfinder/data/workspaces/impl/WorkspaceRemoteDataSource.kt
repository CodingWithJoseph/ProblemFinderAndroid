package com.example.problemfinder.data.workspaces.impl

import com.example.problemfinder.core.model.workspace.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.json.*
import javax.inject.Inject
import javax.inject.Qualifier

@Qualifier @Retention(AnnotationRetention.BINARY) annotation class WorkspaceNetwork
data class WorkspaceApiConfig(val baseUrl: String)

internal class WorkspaceRemoteDataSource @Inject constructor(
    @param:WorkspaceNetwork private val client: HttpClient,
    private val config: WorkspaceApiConfig,
) {
    private fun url(vararg segments: String): String = URLBuilder(config.baseUrl).apply {
        appendPathSegments(*segments, encodeSlash = true)
    }.buildString()
    suspend fun projects(token: String): ProjectsDto = client.get(url("pipeline")) { bearerAuth(token) }.body()
    suspend fun project(id: String, token: String): ProjectDto =
        client.get(url("pipeline", id)) { bearerAuth(token) }.body<ProjectEnvelopeDto>().data
    suspend fun analysis(id: String, token: String): AnalysisDto =
        client.get(url("pipeline", id, "signal", "case")) { bearerAuth(token) }.body()
    suspend fun generateAnalysis(id: String, token: String): AnalysisDto =
        client.post(url("pipeline", id, "signal", "case", "refresh")) { bearerAuth(token) }.body()
    suspend fun updateAnalysis(id: String, kind: String, objectId: String, property: String, value: Boolean, token: String): AnalysisDto =
        client.patch(url("pipeline", id, "signal", "case", "overrides", kind, objectId)) {
            bearerAuth(token); contentType(ContentType.Application.Json)
            setBody(buildJsonObject { put("patch", buildJsonObject { put(property, value) }) })
        }.body()
    suspend fun conversations(id: String, token: String): List<ConversationSummaryDto> =
        client.get(url("pipeline", id, "signal", "conversations")) { bearerAuth(token) }.body()
    suspend fun conversation(id: String, conversationId: String, token: String): ConversationDto =
        client.get(url("pipeline", id, "signal", "conversations", conversationId)) { bearerAuth(token) }.body()
    suspend fun createConversation(id: String, token: String): ConversationDto =
        client.post(url("pipeline", id, "signal", "conversations")) {
            bearerAuth(token); contentType(ContentType.Application.Json); setBody(ConversationCreateDto())
        }.body()
    suspend fun message(id: String, conversationId: String, message: String, token: String): ConversationDto =
        client.post(url("pipeline", id, "signal", "conversations", conversationId, "messages")) {
            bearerAuth(token); contentType(ContentType.Application.Json); setBody(MessageDto(message))
        }.body()
    suspend fun decide(id: String, conversationId: String, proposalId: String, accept: Boolean, token: String): ConversationDto =
        client.patch(url("pipeline", id, "signal", "conversations", conversationId, "proposals", proposalId)) {
            bearerAuth(token); contentType(ContentType.Application.Json); setBody(DecisionDto(if (accept) "accepted" else "rejected"))
        }.body()
    suspend fun solution(id: String, token: String): SolutionDto =
        client.get(url("pipeline", id, "solution", "case")) { bearerAuth(token) }.body()
    suspend fun generateBuild(id: String, request: BuildGeneration, token: String): SolutionDto =
        client.post(url("pipeline", id, "solution", "case", "generate")) {
            bearerAuth(token); contentType(ContentType.Application.Json)
            setBody(GenerationDto(request.targets.map { it.name.lowercase() }, request.constraints, request.preferredStack))
        }.body()
    suspend fun approve(id: String, version: Int, token: String): SolutionDto =
        client.post(url("pipeline", id, "solution", "case", "approve")) {
            bearerAuth(token); contentType(ContentType.Application.Json); setBody(VersionDto(version))
        }.body()
    suspend fun saveDocument(id: String, key: String, version: Int, content: String, token: String): SolutionDto =
        client.patch(url("pipeline", id, "solution", "case", "documents", key)) {
            bearerAuth(token); contentType(ContentType.Application.Json); setBody(DocumentUpdateDto(version, content))
        }.body()
    suspend fun updateTask(id: String, taskId: String, version: Int, update: TaskUpdate, token: String): TaskUpdateResultDto =
        client.patch(url("pipeline", id, "solution", "case", "tasks", taskId)) {
            bearerAuth(token); contentType(ContentType.Application.Json)
            setBody(TaskUpdateDto(version, update.status.name.lowercase(), update.note,
                update.verificationSummary?.let { VerificationDto(it, update.checks.map { check -> VerificationCheckDto(check.command, "passed", check.details) }) }))
        }.body()
    suspend fun export(id: String, token: String): WorkspaceExport {
        val response = client.get(url("pipeline", id, "solution", "case", "export")) { bearerAuth(token) }
        require(response.contentType()?.withoutParameters() == ContentType.Application.Zip)
        val bytes = response.body<ByteArray>()
        require(bytes.isNotEmpty())
        return WorkspaceExport("immensity-build.zip", bytes)
    }
}
