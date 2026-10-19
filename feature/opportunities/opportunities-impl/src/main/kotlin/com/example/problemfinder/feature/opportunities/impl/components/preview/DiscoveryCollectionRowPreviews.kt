package com.example.problemfinder.feature.opportunities.impl.components.preview

import android.content.res.Configuration
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.example.problemfinder.core.designsystem.theme.ImmensityTheme
import com.example.problemfinder.core.model.opportunity.OpportunityCategory
import com.example.problemfinder.core.model.opportunity.Opportunity
import com.example.problemfinder.feature.opportunities.impl.components.DiscoveryCollectionRow
import kotlin.time.Instant

internal data class DiscoveryCollectionPreviewCase(
    val category: OpportunityCategory,
    val clusters: List<Opportunity>,
    val pendingClusterIds: Set<Int> = emptySet(),
)

internal class DiscoveryCollectionPreviewProvider : PreviewParameterProvider<DiscoveryCollectionPreviewCase> {
    private fun cluster(
        id: Int,
        name: String,
        problem: String,
        category: OpportunityCategory,
        postCount: Int,
        watched: Boolean = false,
    ) = Opportunity(
        id = id,
        name = name,
        summary = "A complete opportunity summary for the preview.",
        problemStatement = problem,
        postCount = postCount,
        isWatched = watched,
        date = Instant.parse("2026-10-10T12:00:00Z"),
        category = category,
    )

    override val values = sequenceOf(
        DiscoveryCollectionPreviewCase(
            category = OpportunityCategory.BUILD_AND_TOOLING,
            clusters = listOf(
                cluster(101, "The Next Package", "Package resolution errors prevent teams from completing Turbopack builds.", OpportunityCategory.BUILD_AND_TOOLING, 14),
                cluster(102, "Dry runs that change files", "A command promises a preview but modifies package files instead.", OpportunityCategory.BUILD_AND_TOOLING, 8, watched = true),
                cluster(103, "Finding the broken dependency", "Developers need to trace a build failure through a large dependency graph.", OpportunityCategory.BUILD_AND_TOOLING, 3),
            ),
            pendingClusterIds = setOf(102),
        ),
        DiscoveryCollectionPreviewCase(
            category = OpportunityCategory.DATA_AND_APIS,
            clusters = listOf(
                cluster(201, "Signed in, still locked out", "The next service rejects a session accepted by authentication.", OpportunityCategory.DATA_AND_APIS, 24),
                cluster(202, "Connections without a clear failure", "Unexpected database disconnects leave developers without a useful next step.", OpportunityCategory.DATA_AND_APIS, 5, watched = true),
                cluster(203, "Retries make the outage worse", "Repeated requests overwhelm a service while it is recovering.", OpportunityCategory.DATA_AND_APIS, 2),
            ),
        ),
        DiscoveryCollectionPreviewCase(
            category = OpportunityCategory.WORKFLOW_AND_AUTOMATION,
            clusters = listOf(
                cluster(301, "Automation needs manual recovery", "A background job fails silently and someone has to check every run.", OpportunityCategory.WORKFLOW_AND_AUTOMATION, 11),
                cluster(302, "Context lost between tools", "Teams recreate the same information when moving work to another tool.", OpportunityCategory.WORKFLOW_AND_AUTOMATION, 6),
                cluster(303, "Daily checks that never end", "Routine verification work interrupts the rest of the team's day.", OpportunityCategory.WORKFLOW_AND_AUTOMATION, 1),
            ),
        ),
        DiscoveryCollectionPreviewCase(
            category = OpportunityCategory.UNCATEGORIZED,
            clusters = listOf(
                Opportunity(
                    id = 401,
                    name = "Unclassified opportunity",
                    summary = "A complete opportunity summary for the preview.",
                    problemStatement = "Teams need a reliable way to complete this task.",
                    postCount = 0,
                    isWatched = false,
                    date = null,
                ),
            ),
        ),
        DiscoveryCollectionPreviewCase(
            category = OpportunityCategory.DATA_AND_APIS,
            clusters = emptyList(),
        ),
    )
}

@Preview(name = "Light", widthDp = 393, showBackground = true)
@Preview(name = "Dark", widthDp = 393, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Large text", widthDp = 393, showBackground = true, fontScale = 1.5f)
@Composable
private fun DiscoveryCollectionRowPreview(
    @PreviewParameter(DiscoveryCollectionPreviewProvider::class) case: DiscoveryCollectionPreviewCase,
) = ImmensityTheme {
    Surface(color = MaterialTheme.colorScheme.background) {
        DiscoveryCollectionRow(
            category = case.category,
            clusters = case.clusters,
            pendingClusterIds = case.pendingClusterIds,
            onIntent = {},
            modifier = Modifier.padding(vertical = 16.dp),
        )
    }
}

@Preview(name = "Narrow screen", widthDp = 320, showBackground = true)
@Composable
private fun DiscoveryCollectionRowNarrowPreview() = ImmensityTheme {
    val case = DiscoveryCollectionPreviewProvider().values.first()
    Surface(color = MaterialTheme.colorScheme.background) {
        DiscoveryCollectionRow(
            category = case.category,
            clusters = case.clusters,
            pendingClusterIds = case.pendingClusterIds,
            onIntent = {},
            modifier = Modifier.padding(vertical = 16.dp),
        )
    }
}
