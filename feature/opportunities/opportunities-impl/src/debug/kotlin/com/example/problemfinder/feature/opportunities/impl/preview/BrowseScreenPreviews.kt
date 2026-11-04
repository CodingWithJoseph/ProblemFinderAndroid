package com.example.problemfinder.feature.opportunities.impl.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.problemfinder.core.common.shared.OpportunityCategoryFilter
import com.example.problemfinder.core.designsystem.components.FeaturePreviews
import com.example.problemfinder.core.designsystem.theme.ImmensityTheme
import com.example.problemfinder.core.mvi.RequestState
import com.example.problemfinder.feature.opportunities.impl.browse.BrowseScreen
import com.example.problemfinder.feature.opportunities.impl.browse.BrowseState

private val BrowseExample = BrowseState(
    category = OpportunityCategoryFilter.BUILD_AND_TOOLING,
    userId = "preview-account", data = OpportunityScreenPreviewData.items,
    hasSynced = true, cacheReady = true, observationRequest = RequestState.Success(Unit),
)

@FeaturePreviews
@Composable
private fun BrowseCategoryPreview() = ImmensityTheme { BrowseScreen(BrowseExample, onIntent = {}) }

@Preview(name = "Browse · loading", widthDp = 393, heightDp = 852)
@Composable
private fun BrowseLoadingPreview() = ImmensityTheme { BrowseScreen(BrowseState(), onIntent = {}) }

@Preview(name = "Browse · confirmed empty", widthDp = 393, heightDp = 852)
@Composable
private fun BrowseEmptyPreview() = ImmensityTheme { BrowseScreen(BrowseExample.copy(data = emptyList()), onIntent = {}) }

@Preview(name = "Browse · saved content offline", widthDp = 393, heightDp = 852)
@Composable
private fun BrowseOfflinePreview() = ImmensityTheme {
    BrowseScreen(BrowseExample.copy(refreshRequest = RequestState.Error(OpportunityScreenPreviewData.networkError)), onIntent = {})
}

@Preview(name = "Browse · page retry and pending action", widthDp = 393, heightDp = 1100)
@Composable
private fun BrowsePageRetryPreview() = ImmensityTheme {
    BrowseScreen(BrowseExample.copy(cursor = "preview-next-page", pendingClusterIds = setOf(42),
        pageRequest = RequestState.Error(OpportunityScreenPreviewData.networkError)), onIntent = {})
}
