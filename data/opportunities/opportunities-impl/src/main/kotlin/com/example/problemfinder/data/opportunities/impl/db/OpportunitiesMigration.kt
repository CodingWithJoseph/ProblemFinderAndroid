package com.example.problemfinder.data.opportunities.impl.db

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** Preserves existing cache rows and widens post identity to support server UUIDs. */
internal val opportunitiesMigration1To2 = object : Migration(1, 2) {
    override suspend fun migrate(connection: SQLiteConnection) {
        // The guided branch added Details tables before the database version was bumped.
        val hasLegacyEvidence = connection.prepare(
            "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = 'opportunity_evidence'",
        ).use { it.step() }
        if (hasLegacyEvidence) {
            connection.execSQL("ALTER TABLE opportunity_evidence_membership RENAME TO old_evidence_membership")
            connection.execSQL("ALTER TABLE opportunity_evidence RENAME TO old_evidence")
        }

        connection.execSQL("""
            CREATE TABLE IF NOT EXISTS opportunity_details (
                userId TEXT NOT NULL, clusterId INTEGER NOT NULL, name TEXT NOT NULL,
                summary TEXT NOT NULL, problemStatement TEXT NOT NULL, postCount INTEGER NOT NULL,
                firstSeen TEXT, lastSeen TEXT, authorCount INTEGER NOT NULL, communityCount INTEGER NOT NULL,
                hasSynced INTEGER NOT NULL, pageSize INTEGER NOT NULL, lastLoadedPage INTEGER NOT NULL,
                PRIMARY KEY(userId, clusterId)
            )
        """.trimIndent())
        connection.execSQL("""
            CREATE TABLE IF NOT EXISTS evidence_source_count (
                userId TEXT NOT NULL, clusterId INTEGER NOT NULL, source TEXT NOT NULL, count INTEGER NOT NULL,
                PRIMARY KEY(userId, clusterId, source),
                FOREIGN KEY(userId, clusterId) REFERENCES opportunity_details(userId, clusterId) ON DELETE CASCADE
            )
        """.trimIndent())
        connection.execSQL("""
            CREATE TABLE IF NOT EXISTS evidence_date_count (
                userId TEXT NOT NULL, clusterId INTEGER NOT NULL, date TEXT NOT NULL, count INTEGER NOT NULL,
                PRIMARY KEY(userId, clusterId, date),
                FOREIGN KEY(userId, clusterId) REFERENCES opportunity_details(userId, clusterId) ON DELETE CASCADE
            )
        """.trimIndent())
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
        if (hasLegacyEvidence) {
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
        }
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
