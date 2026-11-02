package com.example.problemfinder.data.workspaces.impl

import com.example.problemfinder.core.model.workspace.*
import com.example.problemfinder.data.authentication.api.AuthSession
import com.example.problemfinder.data.authentication.api.AuthSessionProvider
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.http.content.TextContent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

private const val PROJECT = """{"id":"project-1","userId":"user-a","name":"Build blocker","displayName":"Build blocker","sourceClusterId":"42","stage":"watching","analysisStatus":"ready","buildStatus":"not_started","approvalStatus":"draft"}"""
private const val ANALYSIS = """{"version":1,"status":"ready","project":{"pipelineId":"project-1","projectName":"Build blocker"},"thesis":{"statement":"Supported thesis","confirmed":false},"claims":[],"problemUnits":[{"id":"problem-1","title":"The problem","kind":"core_problem","evidenceIds":["101"],"pinned":false,"rejected":false}],"audiences":[],"assumptions":[],"evidence":[{"id":"101","title":"A source post","excerpt":"The supporting evidence","observedAt":"2026-08-29T12:00:00Z"}]}"""
private val JSON_HEADERS = headersOf(HttpHeaders.ContentType, "application/json")

class WorkspaceRepositoryTest {
    private class Sessions : AuthSessionProvider {
        val owner = MutableStateFlow<String?>("user-a")
        var usable = true
        var credentialReads = 0
        override fun observeUserId() = owner
        override suspend fun currentSession(): AuthSession? {
            credentialReads++
            return owner.value?.takeIf { usable }?.let { AuthSession(it, "test-token") }
        }
    }
    private class Cache : WorkspaceCacheDao {
        val records = MutableStateFlow<Map<Pair<String, String>, WorkspaceCacheEntity>>(emptyMap())
        override fun observe(userId: String, key: String) = records.map { it[userId to key] }
        override suspend fun read(userId: String, key: String) = records.value[userId to key]
        override suspend fun put(record: WorkspaceCacheEntity) { records.update { it + ((record.userId to record.cacheKey) to record) } }
        override suspend fun delete(userId: String, key: String) { records.update { it - (userId to key) } }
    }
    private suspend fun withRepository(
        sessions: Sessions = Sessions(), cache: Cache = Cache(),
        handler: MockRequestHandler,
        block: suspend (WorkspaceRepositoryImpl, WorkspaceRemoteDataSource) -> Unit,
    ) {
        val client = workspaceHttpClient(MockEngine) { addHandler(handler) }
        val remote = WorkspaceRemoteDataSource(client, WorkspaceApiConfig("https://workspace.example"))
        try { block(WorkspaceRepositoryImpl(sessions, remote, cache, Json { ignoreUnknownKeys = true }), remote) }
        finally { client.close() }
    }

    @Test fun readsSavedAnalysisOfflineWithoutRequestingCredentials() = runTest {
        val sessions = Sessions().apply { usable = false }
        val cache = Cache()
        cache.put(WorkspaceCacheEntity("user-a", "analysis:project-1", """{"project":$PROJECT,"analysis":$ANALYSIS}"""))
        withRepository(sessions, cache, { error("Offline observation must not call HTTP") }) { repository, _ ->
            val snapshot = repository.observeAnalysis("project-1").first { it.cacheReady }
            assertEquals("Supported thesis", snapshot.value?.analysis?.thesis?.statement)
            assertEquals(0, sessions.credentialReads)
        }
    }
    @Test fun cacheObservationDropsOldAccountDataAndThenReadsOnlyNewOwner() = runTest {
        val sessions = Sessions()
        val cache = Cache()
        cache.put(WorkspaceCacheEntity("user-a", "projects", """{"data":[$PROJECT]}"""))
        withRepository(sessions, cache, { error("Observation must not call HTTP") }) { repository, _ ->
            assertEquals(1, repository.observeProjects().first { it.cacheReady }.value?.size)
            sessions.owner.value = "user-b"
            val next = repository.observeProjects().first { it.cacheReady }
            assertEquals("user-b", next.userId)
            assertNull(next.value)
            sessions.owner.value = null
            assertNull(repository.observeProjects().first().value)
            assertEquals(0, sessions.credentialReads)
        }
    }
    @Test fun lateOldAccountResponseNeverWritesTheCache() = runTest {
        val sessions = Sessions()
        val cache = Cache()
        val started = CompletableDeferred<Unit>()
        val complete = CompletableDeferred<Unit>()
        withRepository(sessions, cache, { started.complete(Unit); complete.await(); respond("""{"data":[$PROJECT]}""", headers = JSON_HEADERS) }) { repository, _ ->
            val request = async { repository.refreshProjects() }
            started.await(); sessions.owner.value = "user-b"; complete.complete(Unit)
            try { request.await(); fail("Account change must cancel the write") } catch (_: CancellationException) { }
            assertTrue(cache.records.value.isEmpty())
        }
    }
    @Test fun invalidRequiredTextDoesNotReplaceSavedAnalysis() = runTest {
        val cache = Cache()
        val saved = WorkspaceCacheEntity("user-a", "analysis:project-1", """{"project":$PROJECT,"analysis":$ANALYSIS}""")
        cache.put(saved)
        withRepository(cache = cache, handler = { request ->
            if (request.url.encodedPath.endsWith("/signal/case"))
                respond(ANALYSIS.replace("Supported thesis", " "), headers = JSON_HEADERS)
            else respond("""{"data":$PROJECT}""", headers = JSON_HEADERS)
        }) { repository, _ ->
            try { repository.refreshAnalysis("project-1"); fail("Invalid text must fail") }
            catch (error: WorkspaceException) { assertEquals(WorkspaceFailure.INVALID_RESPONSE, error.failure) }
            assertEquals(saved, cache.read("user-a", "analysis:project-1"))
        }
    }
    @Test fun missingAnalysisIsNotStartedOnlyAfterOwnedProjectLookupSucceeds() = runTest {
        withRepository(handler = { request ->
            if (request.url.encodedPath.endsWith("/signal/case")) respond("{}", HttpStatusCode.NotFound, JSON_HEADERS)
            else respond("""{"data":$PROJECT}""", headers = JSON_HEADERS)
        }) { repository, _ ->
            repository.refreshAnalysis("project-1")
            val saved = repository.observeAnalysis("project-1").first { it.cacheReady }
            assertNotNull(saved.value)
            assertNull(saved.value?.analysis)
        }
    }
    @Test fun missingProjectDoesNotBecomeAnEmptySuccessfulAnalysis() = runTest {
        val cache = Cache()
        withRepository(cache = cache, handler = { respond("{}", HttpStatusCode.NotFound, JSON_HEADERS) }) { repository, _ ->
            try { repository.refreshAnalysis("project-1"); fail("Missing project must fail") }
            catch (error: WorkspaceException) { assertEquals(WorkspaceFailure.NOT_FOUND, error.failure) }
            assertTrue(cache.records.value.isEmpty())
        }
    }
    @Test fun generationSendsExplicitTargetsAndCanonicalCamelCase() = runTest {
        withRepository(handler = { request ->
            assertEquals(HttpMethod.Post, request.method)
            assertEquals("/pipeline/project-1/solution/case/generate", request.url.encodedPath)
            assertEquals("Bearer test-token", request.headers[HttpHeaders.Authorization])
            val body = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
            assertEquals(setOf("deliveryTargets", "constraints", "preferredStack"), body.keys)
            assertEquals("android", body["deliveryTargets"]?.jsonArray?.single()?.jsonPrimitive?.content)
            respond("{}", HttpStatusCode.ServiceUnavailable, JSON_HEADERS)
        }) { _, remote ->
            try { remote.generateBuild("project-1", BuildGeneration(setOf(DeliveryTarget.ANDROID)), "test-token"); fail() }
            catch (_: io.ktor.client.plugins.ServerResponseException) { }
        }
    }
    @Test fun documentConflictRetainsBothSavedDocumentAndLocalDraft() = runTest {
        val cache = Cache()
        val saved = WorkspaceCacheEntity("user-a", "build:project-1", "previous valid document")
        val draft = WorkspaceCacheEntity("user-a", "draft:project-1:requirements", """{"key":"requirements","baseVersion":1,"content":"My edit"}""")
        cache.put(saved); cache.put(draft)
        withRepository(cache = cache, handler = { request ->
            if (request.method == HttpMethod.Patch) {
                val body = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
                assertEquals(1, body["expectedVersion"]?.jsonPrimitive?.int)
                assertEquals("My edit", body["content"]?.jsonPrimitive?.content)
                respond("{}", HttpStatusCode.Conflict, JSON_HEADERS)
            } else respond("""{"data":$PROJECT}""", headers = JSON_HEADERS)
        }) { repository, _ ->
            try { repository.saveDocument("project-1", "requirements", 1, "My edit"); fail() }
            catch (error: WorkspaceException) { assertEquals(WorkspaceFailure.CONFLICT, error.failure) }
            assertEquals(saved, cache.read("user-a", "build:project-1"))
            assertEquals(draft, cache.read("user-a", "draft:project-1:requirements"))
        }
    }
    @Test fun taskUpdateUsesAppEndpointAndSuppliesActualVerification() = runTest {
        withRepository(handler = { request ->
            assertEquals("/pipeline/project-1/solution/case/tasks/task-1", request.url.encodedPath)
            assertEquals(HttpMethod.Patch, request.method)
            val body = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
            assertEquals(3, body["expectedVersion"]?.jsonPrimitive?.int)
            assertEquals("done", body["status"]?.jsonPrimitive?.content)
            assertEquals("passed", body["verification"]?.jsonObject?.get("checks")?.jsonArray?.single()?.jsonObject?.get("outcome")?.jsonPrimitive?.content)
            respond("""{"taskId":"task-1","status":"done"}""", headers = JSON_HEADERS)
        }) { _, remote ->
            remote.updateTask("project-1", "task-1", 3, TaskUpdate(TaskProgress.DONE,
                verificationSummary = "Tests passed", checks = listOf(TaskCheck("gradlew test"))), "test-token")
        }
    }
    @Test fun pathSegmentsCannotInjectAnotherRouteOrQuery() = runTest {
        withRepository(handler = { request ->
            assertEquals("/pipeline/project%2Fone%3Fother=two", request.url.encodedPath)
            assertTrue(request.url.parameters.isEmpty())
            respond("""{"data":$PROJECT}""", headers = JSON_HEADERS)
        }) { _, remote -> remote.project("project/one?other=two", "test-token") }
    }
    @Test fun anOldAccountCannotSaveItsDeferredDraftUnderTheNewOwner() = runTest {
        val sessions = Sessions()
        val cache = Cache()
        withRepository(sessions, cache, { error("Drafts are local") }) { repository, _ ->
            sessions.owner.value = "user-b"
            try { repository.saveDocumentDraft("user-a", "project-1", DocumentDraft("requirements", 1, "Private")); fail() }
            catch (_: CancellationException) { }
            assertTrue(cache.records.value.isEmpty())
        }
    }
    @Test fun corruptDerivedCacheIsARecoverableReadAndCanBeReplacedByRefresh() = runTest {
        val cache = Cache()
        cache.put(WorkspaceCacheEntity("user-a", "projects", "obsolete cache shape"))
        withRepository(cache = cache, handler = { respond("""{"data":[$PROJECT]}""", headers = JSON_HEADERS) }) { repository, _ ->
            val initial = repository.observeProjects().first { it.cacheReady }
            assertNull(initial.value)
            assertEquals(WorkspaceFailure.INVALID_RESPONSE, initial.cacheFailure)
            repository.refreshProjects()
            val repaired = repository.observeProjects().first { it.cacheReady }
            assertEquals(1, repaired.value?.size)
            assertNull(repaired.cacheFailure)
        }
    }
    @Test fun successResponseForOldDocumentVersionCannotClearTheLocalDraft() = runTest {
        val cache = Cache()
        val draft = WorkspaceCacheEntity("user-a", "draft:project-1:requirements", "keep this draft")
        cache.put(draft)
        val unchangedVersion = requireNotNull(javaClass.getResourceAsStream("/fixtures/solution-case.json")).bufferedReader().use { it.readText() }
        withRepository(cache = cache, handler = { request ->
            if (request.method == HttpMethod.Patch) respond(unchangedVersion, headers = JSON_HEADERS)
            else respond("""{"data":$PROJECT}""", headers = JSON_HEADERS)
        }) { repository, _ ->
            try { repository.saveDocument("project-1", "requirements", 1, "My edit"); fail() }
            catch (error: WorkspaceException) { assertEquals(WorkspaceFailure.INVALID_RESPONSE, error.failure) }
            assertEquals(draft, cache.read("user-a", "draft:project-1:requirements"))
            assertNull(cache.read("user-a", "build:project-1"))
        }
    }
    @Test fun wrongTaskAcknowledgementCannotMasqueradeAsSuccess() = runTest {
        val cache = Cache()
        withRepository(cache = cache, handler = { request ->
            if (request.method == HttpMethod.Patch) respond("""{"taskId":"another-task","status":"in_progress"}""", headers = JSON_HEADERS)
            else respond("""{"data":$PROJECT}""", headers = JSON_HEADERS)
        }) { repository, _ ->
            try { repository.updateTask("project-1", "task-1", 1, TaskUpdate(TaskProgress.IN_PROGRESS)); fail() }
            catch (error: WorkspaceException) { assertEquals(WorkspaceFailure.INVALID_RESPONSE, error.failure) }
            assertTrue(cache.records.value.isEmpty())
        }
    }
}
