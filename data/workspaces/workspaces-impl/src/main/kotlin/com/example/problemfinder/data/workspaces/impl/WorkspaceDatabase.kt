package com.example.problemfinder.data.workspaces.impl

import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.Query
import androidx.room3.RoomDatabase
import androidx.room3.Upsert
import androidx.room3.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Each immutable backend document is one account-scoped cache record. Replacing a complete
 * document is atomic; its nested evidence/tasks cannot come from different generated versions.
 */
@Entity(tableName = "workspace_documents", primaryKeys = ["userId", "cacheKey"])
internal data class WorkspaceCacheEntity(val userId: String, val cacheKey: String, val payload: String)
@Dao
internal interface WorkspaceCacheDao {
    @Query("SELECT * FROM workspace_documents WHERE userId = :userId AND cacheKey = :key")
    fun observe(userId: String, key: String): Flow<WorkspaceCacheEntity?>
    @Query("SELECT * FROM workspace_documents WHERE userId = :userId AND cacheKey = :key")
    suspend fun read(userId: String, key: String): WorkspaceCacheEntity?
    @Upsert suspend fun put(record: WorkspaceCacheEntity)
    @Query("DELETE FROM workspace_documents WHERE userId = :userId AND cacheKey = :key")
    suspend fun delete(userId: String, key: String)
    @Transaction
    suspend fun saveEditedDocument(record: WorkspaceCacheEntity, draftKey: String) {
        put(record)
        delete(record.userId, draftKey)
    }
}
@Database(entities = [WorkspaceCacheEntity::class], version = 1, exportSchema = true)
internal abstract class WorkspaceDatabase : RoomDatabase() {
    abstract fun documents(): WorkspaceCacheDao
}
