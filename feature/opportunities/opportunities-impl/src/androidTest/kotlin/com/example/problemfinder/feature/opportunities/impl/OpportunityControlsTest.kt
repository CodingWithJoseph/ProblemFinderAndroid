package com.example.problemfinder.feature.opportunities.impl

import android.graphics.Bitmap
import android.content.ContentValues
import android.content.res.Configuration
import android.provider.MediaStore
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.problemfinder.core.common.shared.OpportunityCategoryFilter
import com.example.problemfinder.core.common.shared.SortType
import com.example.problemfinder.core.designsystem.theme.ImmensityTheme
import com.example.problemfinder.core.model.opportunity.Opportunity
import com.example.problemfinder.core.model.opportunity.OpportunityCategory
import com.example.problemfinder.core.model.discovery.DiscoveryCollection
import com.example.problemfinder.core.mvi.RequestState
import com.example.problemfinder.feature.opportunities.impl.browse.BrowseIntent
import com.example.problemfinder.feature.opportunities.impl.browse.BrowseScreen
import com.example.problemfinder.feature.opportunities.impl.browse.BrowseState
import com.example.problemfinder.feature.opportunities.impl.discovery.DiscoveryIntent
import com.example.problemfinder.feature.opportunities.impl.discovery.DiscoveryScreen
import com.example.problemfinder.feature.opportunities.impl.discovery.DiscoveryState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Instant

@RunWith(AndroidJUnit4::class)
class OpportunityControlsTest {
    @get:Rule val compose = createComposeRule()
    private val card = Opportunity(42, "When the build becomes the blocker", "Package resolution errors prevent teams from shipping.", "Package resolution errors keep Turbopack projects from shipping.", 14, false, Instant.parse("2026-10-10T12:00:00Z"), OpportunityCategory.BUILD_AND_TOOLING)

    @Test fun filterAndSortMenuChangesTheVisibleQueryAndRefreshIsAnIconAction() {
        val state = mutableStateOf(ready())
        var refreshes = 0
        compose.setContent { ImmensityTheme { BrowseScreen(state.value, onIntent = { intent ->
            when (intent) {
                is BrowseIntent.ChangeCategory -> state.value = state.value.copy(category = intent.value)
                is BrowseIntent.ChangeSortType -> state.value = state.value.copy(sort = intent.value)
                BrowseIntent.Reload -> refreshes++
                else -> {}
            }
        }) } }
        compose.onNodeWithContentDescription("Filter and sort").performClick()
        compose.onNodeWithText("Data & APIs").performClick()
        compose.onNodeWithText("Data & APIs").assertIsDisplayed()
        compose.onNodeWithContentDescription("Filter and sort").performClick()
        compose.onNodeWithText("Oldest").performClick()
        compose.onNodeWithText("Oldest").assertIsDisplayed()
        compose.onNodeWithContentDescription("Refresh").performClick()
        compose.runOnIdle { assertEquals(1, refreshes) }
        saveScreenshot("browse-controls.png")
    }

    @Test fun rowOpenAndAddRemoveAreIndependentAccessibleActions() {
        val state = mutableStateOf(ready())
        val events = mutableListOf<BrowseIntent>()
        compose.setContent { ImmensityTheme { BrowseScreen(state.value, onIntent = { events += it }) } }
        compose.onNodeWithContentDescription("Add ${card.name}").performClick()
        compose.runOnIdle { assertEquals(listOf(BrowseIntent.WatchCluster(42)), events) }
        compose.onNodeWithText(card.name).performClick()
        compose.runOnIdle { assertEquals(BrowseIntent.OpenCluster(42), events.last()) }
        compose.runOnIdle { state.value = ready().copy(data = listOf(card.copy(isWatched = true)), pendingClusterIds = setOf(42)) }
        compose.onNodeWithContentDescription("Remove ${card.name}").assertIsNotEnabled()
        compose.runOnIdle { state.value = state.value.copy(pendingClusterIds = emptySet()) }
        compose.onNodeWithContentDescription("Remove ${card.name}").performClick()
        compose.runOnIdle { assertEquals(BrowseIntent.WatchCluster(42), events.last()) }
    }

    @Test fun largeTextKeepsRefreshMenuAndRowActionAccessible() {
        compose.setContent { ImmensityTheme {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                BrowseScreen(ready(), onIntent = {})
            }
        } }
        compose.onNodeWithContentDescription("Refresh").assertIsDisplayed()
        compose.onNodeWithContentDescription("Filter and sort").assertIsDisplayed()
        compose.onNodeWithContentDescription("Add ${card.name}").assertIsDisplayed()
        saveScreenshot("browse-large-text.png")
    }

    @Test fun dashboardRefreshAndFeaturedActionEmitTheirOwnIntents() {
        val events = mutableListOf<DiscoveryIntent>()
        compose.setContent { ImmensityTheme {
            DiscoveryScreen(DiscoveryState(userId = "user", featuredCluster = card, collections = listOf(DiscoveryCollection(OpportunityCategory.BUILD_AND_TOOLING, emptyList())), cacheReady = true, hasSynced = true, observationRequest = RequestState.Success(Unit)), onIntent = { events += it })
        } }
        compose.onNodeWithContentDescription("Refresh").performClick()
        compose.onNodeWithContentDescription("Add ${card.name}").performClick()
        compose.runOnIdle { assertEquals(listOf(DiscoveryIntent.Reload, DiscoveryIntent.WatchCluster(42)), events) }
        saveScreenshot("discovery-controls.png")
    }

    @Test fun darkThemeKeepsTheSameActionsVisible() {
        compose.setContent {
            val configuration = Configuration(LocalConfiguration.current).apply {
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or Configuration.UI_MODE_NIGHT_YES
            }
            CompositionLocalProvider(LocalConfiguration provides configuration) {
                ImmensityTheme { BrowseScreen(ready(), onIntent = {}) }
            }
        }
        compose.onNodeWithContentDescription("Refresh").assertIsDisplayed()
        compose.onNodeWithContentDescription("Filter and sort").assertIsDisplayed()
        compose.onNodeWithContentDescription("Add ${card.name}").assertIsDisplayed()
        saveScreenshot("browse-dark.png")
    }

    private fun ready() = BrowseState(category = OpportunityCategoryFilter.BUILD_AND_TOOLING, userId = "user", data = listOf(card), cacheReady = true, hasSynced = true, observationRequest = RequestState.Success(Unit))
    private fun saveScreenshot(name: String) {
        if (InstrumentationRegistry.getArguments().getString("captureScreenshots") != "true") return
        val image = compose.onRoot().captureToImage().asAndroidBitmap()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CodexBrowseChecks")
            }
            val uri = requireNotNull(context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values))
            context.contentResolver.openOutputStream(uri)!!.use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}
