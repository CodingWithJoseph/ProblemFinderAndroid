package com.example.problemfinder.data.opportunities.impl.db

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** Preserves existing cache rows and widens post identity to support server UUIDs. */
internal val opportunitiesMigration1To2 = object : Migration(1, 2) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE opportunity_evidence_membership RENAME TO old_evidence_membership")
        connection.execSQL("ALTER TABLE opportunity_evidence RENAME TO old_evidence")
        connection.execSQL("""
            CREATE TABLE opportunity_evidence (
                userId TEXT NOT NULL, clusterId INTEGER NOT NULL, postId TEXT NOT NULL,
                title TEXT NOT NULL, body TEXT, url TEXT, author TEXT, community TEXT, source TEXT,
                score INTEGER, commentCount INTEGER, postedAt TEXT, problemStatement TEXT,
                PRIMARY KEY(userId, clusterId, postId),
                FOREIGN KEY(userId, clusterId) REFERENCES opportunity_details(userId, clusterId) ON DELETE CASCADE
            )
        """.trimIndent())
        connection.execSQL("""
            CREATE TABLE opportunity_evidence_membership (
                userId TEXT NOT NULL, clusterId INTEGER NOT NULL, postId TEXT NOT NULL,
                section TEXT NOT NULL, page INTEGER NOT NULL, position INTEGER NOT NULL,
                PRIMARY KEY(userId, clusterId, postId, section),
                FOREIGN KEY(userId, clusterId) REFERENCES opportunity_details(userId, clusterId) ON DELETE CASCADE,
                FOREIGN KEY(userId, clusterId, postId)
                    REFERENCES opportunity_evidence(userId, clusterId, postId) ON DELETE CASCADE
            )
        """.trimIndent())
        connection.execSQL("""
            INSERT INTO opportunity_evidence
            SELECT userId, clusterId, CAST(postId AS TEXT), title, body, url, author, community,
                source, score, commentCount, postedAt, problemStatement
            FROM old_evidence
        """.trimIndent())
        connection.execSQL("""
            INSERT INTO opportunity_evidence_membership
            SELECT userId, clusterId, CAST(postId AS TEXT), section, page, position FROM old_evidence_membership
        """.trimIndent())
        connection.execSQL("DROP TABLE old_evidence_membership")
        connection.execSQL("DROP TABLE old_evidence")
        connection.execSQL("""
            CREATE INDEX index_opportunity_evidence_membership_userId_clusterId_section_page_position
            ON opportunity_evidence_membership(userId, clusterId, section, page, position)
        """.trimIndent())
        connection.execSQL("""
            CREATE TABLE search_queries (
                userId TEXT NOT NULL, queryKey TEXT NOT NULL, total INTEGER NOT NULL, nextOffset INTEGER,
                PRIMARY KEY(userId, queryKey)
            )
        """.trimIndent())
        connection.execSQL("""
            CREATE TABLE search_query_items (
                userId TEXT NOT NULL, queryKey TEXT NOT NULL, clusterId INTEGER NOT NULL, position INTEGER NOT NULL,
                PRIMARY KEY(userId, queryKey, clusterId),
                FOREIGN KEY(userId, queryKey) REFERENCES search_queries(userId, queryKey) ON DELETE CASCADE,
                FOREIGN KEY(userId, clusterId) REFERENCES opportunities(userId, clusterId) ON DELETE CASCADE
            )
        """.trimIndent())
        connection.execSQL("CREATE INDEX index_search_query_items_userId_clusterId ON search_query_items(userId, clusterId)")
        connection.execSQL("CREATE INDEX index_search_query_items_userId_queryKey_position ON search_query_items(userId, queryKey, position)")
    }
}
