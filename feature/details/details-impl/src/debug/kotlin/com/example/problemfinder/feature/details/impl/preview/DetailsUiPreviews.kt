package com.example.problemfinder.feature.details.impl.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.example.problemfinder.core.designsystem.components.FeatureContentStatus
import com.example.problemfinder.core.designsystem.components.FeaturePreviews
import com.example.problemfinder.core.designsystem.theme.ImmensityTheme
import com.example.problemfinder.core.model.details.*
import com.example.problemfinder.core.model.opportunity.Opportunity
import com.example.problemfinder.core.model.opportunity.OpportunityCategory
import com.example.problemfinder.core.model.opportunity.OpportunityFailure
import com.example.problemfinder.core.mvi.RequestState
import com.example.problemfinder.feature.details.impl.details.*
import com.example.problemfinder.feature.details.impl.post.*
import com.example.problemfinder.feature.details.impl.util.OpportunityDetailsUIError
import kotlin.time.Instant

/** Debug-only preview fixtures; no runtime ViewModel or repository receives them. */
internal object DetailsUiPreviewData {
    val post = OpportunityEvidence(
        101, "Turbopack cannot find the Next.js package",
        "The development server stops while resolving the package. Reinstalling dependencies helps temporarily, " +
            "but the same failure returns on the next build.\n\nThe team needs an explanation that identifies the " +
            "missing dependency and the workspace configuration responsible for the failure.",
        "https://github.com/vercel/next.js/issues", "build_runner", "vercel/next.js", "GitHub",
        18, 7, Instant.parse("2026-08-29T13:30:00Z"),
        "Package resolution errors block an otherwise valid project.",
    )
    val details = OpportunityDetails(
        42, "When the build becomes the blocker",
        "Package resolution failures stop Next.js teams before they can ship.",
        "Developers report that Turbopack cannot resolve a package or write an app endpoint, even when the " +
            "project previously built successfully.\n\nWithout a useful explanation, teams rebuild dependencies " +
            "and try workarounds instead of shipping their changes.",
        14, Instant.parse("2026-08-03T10:00:00Z"), Instant.parse("2026-08-29T13:30:00Z"), 11, 4,
        listOf(EvidenceSourceCount("GitHub", 8), EvidenceSourceCount("Stack Overflow", 4), EvidenceSourceCount("Reddit", 2)),
        listOf(EvidenceDateCount("2026-08-03", 1), EvidenceDateCount("2026-08-11", 4), EvidenceDateCount("2026-08-29", 9)),
    )
    val state = OpportunityDetailsState(
        clusterId = 42, userId = "preview-account", details = details,
        cachedOpportunity = Opportunity(42, details.name, details.summary, details.problemStatement,
            14, false, details.lastSeen, OpportunityCategory.BUILD_AND_TOOLING),
        topEvidence = listOf(post),
        posts = listOf(post, post.copy(id = 102, title = "A workspace change breaks endpoint generation")),
        canLoadMore = true, cacheReady = true, hasSynced = true,
        observationRequest = RequestState.Success(Unit), implementationPending = false,
    )
}

@FeaturePreviews
@Composable
private fun DetailsOverviewPreview() = ImmensityTheme {
    OpportunityDetailsScreen(DetailsUiPreviewData.state, {})
}

@FeaturePreviews
@Composable
private fun EvidenceListPreview() = ImmensityTheme {
    OpportunityDetailsScreen(DetailsUiPreviewData.state, {}, section = DetailsSection.EVIDENCE)
}

@FeaturePreviews
@Composable
private fun PostDetailsPreview() = ImmensityTheme {
    PostDetailsScreen(PostDetailsState(42, 101, DetailsUiPreviewData.post, FeatureContentStatus.CONTENT), {})
}

internal class DetailsStatePreviews : PreviewParameterProvider<OpportunityDetailsState> {
    override val values = sequenceOf(
        OpportunityDetailsState(observationRequest = RequestState.Loading),
        OpportunityDetailsState(clusterId = 42, cacheReady = true),
        OpportunityDetailsState(clusterId = 42, cacheReady = true,
            refreshRequest = RequestState.Error(OpportunityDetailsUIError(OpportunityFailure.CLUSTER_NOT_FOUND))),
        DetailsUiPreviewData.state.copy(refreshRequest = RequestState.Error(OpportunityDetailsUIError(OpportunityFailure.NETWORK))),
        DetailsUiPreviewData.state.copy(isWatchPending = true),
        DetailsUiPreviewData.state.copy(cachedOpportunity = null),
        DetailsUiPreviewData.state.copy(posts = emptyList(), topEvidence = emptyList()),
    )
}

@Preview(name = "Details states", widthDp = 393, heightDp = 852)
@Composable
private fun DetailsStatesPreview(@PreviewParameter(DetailsStatePreviews::class) state: OpportunityDetailsState) =
    ImmensityTheme { OpportunityDetailsScreen(state, {}) }

@Preview(name = "Evidence retry", widthDp = 393, heightDp = 1100)
@Composable
private fun EvidenceRetryPreview() = ImmensityTheme {
    OpportunityDetailsScreen(DetailsUiPreviewData.state.copy(
        pageRequest = RequestState.Error(OpportunityDetailsUIError(OpportunityFailure.NETWORK))
    ), {}, section = DetailsSection.EVIDENCE)
}

@Preview(name = "Post without body or link", widthDp = 393, heightDp = 852)
@Composable
private fun SparsePostPreview() = ImmensityTheme {
    PostDetailsScreen(PostDetailsState(post = DetailsUiPreviewData.post.copy(
        body = null, url = null, author = null, score = null, commentCount = null
    ), status = FeatureContentStatus.CONTENT), {})
}
