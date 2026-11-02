package com.example.problemfinder.feature.search.impl.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.example.problemfinder.core.designsystem.components.FeaturePreviews
import com.example.problemfinder.core.designsystem.theme.ImmensityTheme
import com.example.problemfinder.core.model.opportunity.*
import com.example.problemfinder.core.model.search.*
import com.example.problemfinder.core.mvi.RequestState
import com.example.problemfinder.feature.search.impl.*

/** Debug-only data for Compose previews. */
internal object SearchUiPreviewData {
    val query = OpportunitySearchQuery(query = "developer build failures")
    val opportunity = Opportunity(42, "When the build becomes the blocker",
        "Package resolution failures stop teams before they can ship.",
        "The build tool fails without explaining which dependency is missing.",
        14, false, null, OpportunityCategory.BUILD_AND_TOOLING)
    val results = SearchState(
        draftQuery = query, submittedQuery = query, userId = "preview-account",
        items = listOf(opportunity, opportunity.copy(id = 43, name = "Signed in, still locked out",
            category = OpportunityCategory.DATA_AND_APIS, isWatched = true)),
        total = 24, canLoadMore = true, cacheReady = true, hasSynced = true,
        observationRequest = RequestState.Success(Unit), implementationPending = false,
        sourceOptions = listOf("GitHub", "Stack Overflow", "Reddit"),
        communityOptions = listOf("next.js", "supabase", "r/webdev"),
    )
    val draft = OpportunitySearchDraft(query,
        "Find developer build failures, ordered by relevance.",
        listOf("Focus on recurring problems rather than release announcements."),
        listOf("Industry filters are not part of this search."),
        null, false, true, false)
}

@FeaturePreviews
@Composable
private fun SearchResultsPreview() = ImmensityTheme { SearchScreen(SearchUiPreviewData.results, {}) }

@FeaturePreviews
@Composable
private fun SearchFiltersPreview() = ImmensityTheme { SearchFiltersScreen(SearchUiPreviewData.results, {}) }

@FeaturePreviews
@Composable
private fun SearchAssistantPreview() = ImmensityTheme {
    SearchAssistantScreen(SearchUiPreviewData.results.copy(interpretation = SearchUiPreviewData.draft), {})
}

internal class SearchStatePreviews : PreviewParameterProvider<SearchState> {
    override val values = sequenceOf(
        SearchState(implementationPending = false),
        SearchState(draftQuery = SearchUiPreviewData.query, refreshRequest = RequestState.Loading),
        SearchState(submittedQuery = SearchUiPreviewData.query, cacheReady = true, hasSynced = true),
        SearchState(refreshRequest = RequestState.Error(OpportunityFailure.NETWORK)),
        SearchUiPreviewData.results.copy(refreshRequest = RequestState.Error(OpportunityFailure.NETWORK)),
        SearchUiPreviewData.results.copy(pageRequest = RequestState.Error(OpportunityFailure.NETWORK)),
        SearchUiPreviewData.results.copy(pendingClusterIds = setOf(42)),
        SearchState(validationMessage = "Enter a more specific phrase.", implementationPending = false),
    )
}

@Preview(name = "Search states", widthDp = 393, heightDp = 852)
@Composable
private fun SearchStatesPreview(@PreviewParameter(SearchStatePreviews::class) state: SearchState) =
    ImmensityTheme { SearchScreen(state, {}) }

@Preview(name = "Assistant clarification", widthDp = 393, heightDp = 1100)
@Composable
private fun ClarificationPreview() = ImmensityTheme {
    SearchAssistantScreen(SearchUiPreviewData.results.copy(interpretation = SearchUiPreviewData.draft.copy(
        clarificationQuestion = "Are you interested in local development or production deployments?",
        needsClarification = true,
    )), {})
}
