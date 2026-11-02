package com.example.problemfinder.data.workspaces.impl

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class WorkspaceCacheTest {
    private fun context(): Context = ApplicationProvider.getApplicationContext()
    private fun open(name: String) = Room.databaseBuilder<WorkspaceDatabase>(context(), name)
        .setDriver(AndroidSQLiteDriver()).build()
    private inline fun <T> withDatabase(database: WorkspaceDatabase, block: (WorkspaceDatabase) -> T): T =
        try { block(database) } finally { database.close() }

    @Test fun accountAndProjectRecordsStayIsolatedAcrossDatabaseReopen() = runBlocking {
        val name = "workspace-test-" + UUID.randomUUID() + ".db"
        try {
            withDatabase(open(name)) { database ->
                val dao = database.documents()
                dao.put(WorkspaceCacheEntity("user-a", "analysis:one", "A1"))
                dao.put(WorkspaceCacheEntity("user-a", "analysis:two", "A2"))
                dao.put(WorkspaceCacheEntity("user-b", "analysis:one", "B1"))
            }
            withDatabase(open(name)) { database ->
                assertEquals("A1", database.documents().observe("user-a", "analysis:one").first()?.payload)
                assertEquals("A2", database.documents().read("user-a", "analysis:two")?.payload)
                assertEquals("B1", database.documents().read("user-b", "analysis:one")?.payload)
                assertNull(database.documents().read("user-c", "analysis:one"))
            }
        } finally { context().deleteDatabase(name) }
    }
    @Test fun savingDocumentClearsOnlyItsOwnersMatchingDraft() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder<WorkspaceDatabase>(context()).setDriver(AndroidSQLiteDriver()).build()
        withDatabase(database) {
            val dao = it.documents()
            dao.put(WorkspaceCacheEntity("user-a", "draft:one:requirements", "A draft"))
            dao.put(WorkspaceCacheEntity("user-a", "draft:one:tasks", "Another document"))
            dao.put(WorkspaceCacheEntity("user-b", "draft:one:requirements", "B draft"))
            dao.saveEditedDocument(WorkspaceCacheEntity("user-a", "build:one", "Version 2"), "draft:one:requirements")
            assertEquals("Version 2", dao.read("user-a", "build:one")?.payload)
            assertNull(dao.read("user-a", "draft:one:requirements"))
            assertEquals("Another document", dao.read("user-a", "draft:one:tasks")?.payload)
            assertEquals("B draft", dao.read("user-b", "draft:one:requirements")?.payload)
        }
    }
}
