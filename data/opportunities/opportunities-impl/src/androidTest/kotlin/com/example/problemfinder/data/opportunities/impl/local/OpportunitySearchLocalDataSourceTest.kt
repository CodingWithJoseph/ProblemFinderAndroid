package com.example.problemfinder.data.opportunities.impl.local

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.problemfinder.data.opportunities.impl.db.opportunitiesMigration1To2
import com.example.problemfinder.core.model.opportunity.Opportunity
import com.example.problemfinder.core.model.search.OpportunitySearchPage
import com.example.problemfinder.core.model.search.OpportunitySearchQuery
import com.example.problemfinder.data.opportunities.impl.db.OpportunitiesDatabase
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OpportunitySearchLocalDataSourceTest {
    @Test
    fun upgradesGuidedVersionOneAndPreservesEvidence() = verifyUpgrade(hasDetails = true)

    @Test
    fun upgradesMainVersionOneWithoutDetailsTables() = verifyUpgrade(hasDetails = false)

    private fun verifyUpgrade(hasDetails: Boolean) = runBlocking {
        val detailsTables = setOf("opportunity_details", "opportunity_evidence", "opportunity_evidence_membership",
            "evidence_source_count", "evidence_date_count")
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "search-migration-" + UUID.randomUUID()
        val path = context.getDatabasePath(name).apply { parentFile?.mkdirs() }
        val driver = AndroidSQLiteDriver()
        val schemaPath = OpportunitiesDatabase::class.java.name + "/1.json"
        val schema = InstrumentationRegistry.getInstrumentation().context.assets.open(schemaPath).bufferedReader().use {
            JSONObject(it.readText()).getJSONObject("database")
        }
        driver.open(path.absolutePath).use { connection ->
            val entities = schema.getJSONArray("entities")
            for (index in 0 until entities.length()) {
                val entity = entities.getJSONObject(index)
                val table = entity.getString("tableName")
                if (!hasDetails && table in detailsTables) continue
                val placeholder = "$" + "{TABLE_NAME}"
                connection.execSQL(entity.getString("createSql").replace(placeholder, table))
                val indices = entity.optJSONArray("indices")
                if (indices != null) for (i in 0 until indices.length()) {
                    connection.execSQL(indices.getJSONObject(i).getString("createSql").replace(placeholder, table))
                }
            }
            val setup = schema.getJSONArray("setupQueries")
            for (index in 0 until setup.length()) connection.execSQL(setup.getString(index))
            if (!hasDetails) connection.execSQL("UPDATE room_master_table SET identity_hash = '27ab01d7db12eb3af61b1c2d9b6cbdb1' WHERE id = 42")
            connection.execSQL("PRAGMA user_version = 1")
            connection.execSQL("""
                INSERT INTO opportunities
                    (userId, clusterId, name, summary, problemStatement, postCount, category, isWatched)
                VALUES ('account', 1, 'Name', 'Summary', 'Problem', 1, 'data_and_apis', 1)
            """.trimIndent())
            if (hasDetails) {
                connection.execSQL("""
                    INSERT INTO opportunity_details
                        (userId, clusterId, name, summary, problemStatement, postCount, authorCount,
                         communityCount, hasSynced, pageSize, lastLoadedPage)
                    VALUES ('account', 1, 'Name', 'Summary', 'Problem', 1, 0, 0, 1, 20, 1)
                """.trimIndent())
                connection.execSQL("""
                    INSERT INTO opportunity_evidence (userId, clusterId, postId, title)
                    VALUES ('account', 1, 101, 'Cached post')
                """.trimIndent())
                connection.execSQL("""
                    INSERT INTO opportunity_evidence_membership (userId, clusterId, postId, section, page, position)
                    VALUES ('account', 1, 101, 'posts', 1, 0)
                """.trimIndent())
            }
        }
        val db = Room.databaseBuilder<OpportunitiesDatabase>(context, name).setDriver(driver)
            .addMigrations(opportunitiesMigration1To2).build()
        try {
            val local = OpportunitySearchLocalDataSource(db)
            val details = OpportunityDetailsLocalDataSource(db, db.discoveryDetailsDao())
            val cachedPosts = details.observeDetails("account", 1).first().posts
            assertEquals(if (hasDetails) listOf("101") else emptyList(), cachedPosts.map { it.id })
            val query = OpportunitySearchQuery(query = "first query")
            val other = OpportunitySearchQuery(query = "other query")
            assertTrue(requireNotNull(db.opportunityDao().find("account", 1)).isWatched)
            assertFalse(local.observeSearch("account", query).first().hasSynced)
            val one = card(1)
            val two = card(2)
            val three = card(3)
            local.storePage("account", query, OpportunitySearchPage(listOf(two, one), query, 4, 20), replace = true)
            local.storePage("account", other, OpportunitySearchPage(emptyList(), other, 0, null), replace = true)
            local.storePage("account", query, OpportunitySearchPage(listOf(one, three), query, 4, null), replace = false)
            db.opportunityDao().setWatched("account", 1, true)
            val result = local.observeSearch("account", query).first()
            assertEquals(listOf(2, 1, 3), result.items.map { it.id })
            assertTrue(result.items[1].isWatched)
            assertEquals("DATA_AND_APIS", result.items[1].category.name)
            assertNull(result.nextOffset)
            assertTrue(local.observeSearch("account", other).first().hasSynced)
            assertFalse(local.observeSearch("other-account", query).first().hasSynced)
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }

    private fun card(id: Int) = Opportunity(id, "Name", "Summary", "Problem", 1, false, null)
}
