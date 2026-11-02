package com.example.problemfinder.feature.opportunities.impl.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.problemfinder.core.designsystem.components.FeaturePreviews
import com.example.problemfinder.core.designsystem.theme.ImmensityTheme
import com.example.problemfinder.core.model.discovery.DiscoveryCollection
import com.example.problemfinder.core.model.opportunity.Opportunity
import com.example.problemfinder.core.model.opportunity.OpportunityCategory
import com.example.problemfinder.core.model.opportunity.OpportunityFailure
import com.example.problemfinder.core.mvi.RequestState
import com.example.problemfinder.feature.opportunities.impl.discovery.DiscoveryScreen
import com.example.problemfinder.feature.opportunities.impl.discovery.DiscoveryState
import com.example.problemfinder.feature.opportunities.impl.util.OpportunityUIError
import kotlin.time.Instant

/** Preview fixtures only. All layouts below call the real screen, with no ViewModel or HTTP work. */
internal object OpportunityScreenPreviewData {
    val build = Opportunity(
        id = 42,
        name = "When the build becomes the blocker",
        summary = "Package resolution failures stop teams before they can ship.",
        problemStatement = "Developers cannot identify which dependency broke their build.",
        postCount = 14,
        isWatched = false,
        date = Instant.parse("2026-08-29T13:30:00Z"),
        category = OpportunityCategory.BUILD_AND_TOOLING,
    )
    val items = listOf(
        build,
        build.copy(id = 43, name = "Dry runs that change files", isWatched = true,
            summary = "A command promises a preview but modifies package files instead."),
        build.copy(id = 44, name = "Finding the broken dependency",
            summary = "A large dependency graph hides the cause of a build failure."),
    )
    val discovery = DiscoveryState(
        userId = "preview-account", featuredCluster = build,
        collections = listOf(
            DiscoveryCollection(OpportunityCategory.BUILD_AND_TOOLING, items),
            DiscoveryCollection(OpportunityCategory.DATA_AND_APIS, listOf(
                build.copy(id = 45, name = "Signed in, still locked out",
                    summary = "The next service rejects a session accepted by authentication.",
                    category = OpportunityCategory.DATA_AND_APIS),
                build.copy(id = 46, name = "Retries make the outage worse",
                    category = OpportunityCategory.DATA_AND_APIS),
            )),
            DiscoveryCollection(OpportunityCategory.WORKFLOW_AND_AUTOMATION, listOf(
                build.copy(id = 47, name = "Automation needs manual recovery",
                    summary = "Silent background failures require someone to check every run.",
                    category = OpportunityCategory.WORKFLOW_AND_AUTOMATION),
            )),
        ),
        hasSynced = true, cacheReady = true, observationRequest = RequestState.Success(Unit),
    )
    val networkError = OpportunityUIError(OpportunityFailure.NETWORK)
}

@FeaturePreviews
@Composable
private fun DiscoveryScreenPreview() = ImmensityTheme {
    DiscoveryScreen(OpportunityScreenPreviewData.discovery, {})
}

@Preview(name = "Discovery · full page", widthDp = 393, heightDp = 2200, showBackground = true)
@Composable
private fun DiscoveryFullPagePreview() = ImmensityTheme {
    DiscoveryScreen(OpportunityScreenPreviewData.discovery, {})
}

@Preview(name = "Discovery · loading", widthDp = 393, heightDp = 852)
@Composable
private fun DiscoveryLoadingPreview() = ImmensityTheme {
    DiscoveryScreen(DiscoveryState(), {})
}

@Preview(name = "Discovery · confirmed empty", widthDp = 393, heightDp = 852)
@Composable
private fun DiscoveryEmptyPreview() = ImmensityTheme {
    DiscoveryScreen(OpportunityScreenPreviewData.discovery.copy(featuredCluster = null, collections = emptyList()), {})
}

@Preview(name = "Discovery · saved content offline", widthDp = 393, heightDp = 852)
@Composable
private fun DiscoveryOfflinePreview() = ImmensityTheme {
    DiscoveryScreen(OpportunityScreenPreviewData.discovery.copy(
        refreshRequestState = RequestState.Error(OpportunityScreenPreviewData.networkError),
    ), {})
}

@Preview(name = "Discovery · refreshing and adding", widthDp = 393, heightDp = 852)
@Composable
private fun DiscoveryPendingPreview() = ImmensityTheme {
    DiscoveryScreen(OpportunityScreenPreviewData.discovery.copy(
        refreshRequestState = RequestState.Loading, pendingClusterIds = setOf(42),
    ), {})
}
