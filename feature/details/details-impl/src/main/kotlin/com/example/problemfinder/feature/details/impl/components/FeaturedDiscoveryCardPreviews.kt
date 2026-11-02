package com.example.problemfinder.feature.details.impl.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.example.problemfinder.core.designsystem.theme.ImmensityTheme
import com.example.problemfinder.core.model.opportunity.OpportunityCategory
import com.example.problemfinder.core.model.opportunity.Opportunity
import kotlin.time.Instant
import androidx.compose.ui.unit.dp

internal class FeaturedCardPreviewProvider : PreviewParameterProvider<OpportunityCardPreviewCase> {

    override val values = OpportunityCardPreviewProvider().values.map { case ->
        if (case.cluster.id == 1) {
            case.copy(
                cluster = case.cluster.copy(
                    name = "When the build becomes the blocker",
                    summary = "Package resolution errors keep Turbopack projects from shipping.",
                    problemStatement = "Package resolution errors keep Turbopack projects from shipping.",
                ),
            )
        } else {
            case
        }
    }
}

@Preview(name = "Light", widthDp = 393, showBackground = true)
@Preview(name = "Dark", widthDp = 393, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Large text", widthDp = 393, showBackground = true, fontScale = 1.5f)
@Composable
private fun FeaturedDiscoveryCardPreview(
    @PreviewParameter(FeaturedCardPreviewProvider::class) case: OpportunityCardPreviewCase,
) = ImmensityTheme {
    Surface(color = MaterialTheme.colorScheme.background) {
        FeaturedDiscoveryCard(
            cluster = case.cluster,
            isActionPending = case.pending,
            onIntent = {},
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
        )
    }
}

@Preview(name = "Narrow screen", widthDp = 320, showBackground = true)
@Preview(name = "Double text size", widthDp = 393, showBackground = true, fontScale = 2f)
@Composable
private fun FeaturedDiscoveryCardCompactPreview() = ImmensityTheme {
    val case = FeaturedCardPreviewProvider().values.first()
    Surface(color = MaterialTheme.colorScheme.background) {
        FeaturedDiscoveryCard(
            cluster = case.cluster,
            isActionPending = case.pending,
            onIntent = {},
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
        )
    }
}

internal data class OpportunityCardPreviewCase(
    val cluster: Opportunity,
    val pending: Boolean = false,
)

internal class OpportunityCardPreviewProvider : PreviewParameterProvider<OpportunityCardPreviewCase> {
    private val build = Opportunity(
        id = 1,
        name = "The Next Package",
        summary = "Package resolution errors prevent teams from completing Turbopack builds.",
        problemStatement = "Turbopack fails to identify the Next.js package and cannot write the app endpoint. Teams need a reliable way to diagnose missing package references.",
        postCount = 14,
        isWatched = false,
        date = Instant.parse("2026-10-10T12:00:00Z"),
        category = OpportunityCategory.BUILD_AND_TOOLING,
    )

    override val values = sequenceOf(
        OpportunityCardPreviewCase(build),
        OpportunityCardPreviewCase(
            build.copy(
                id = 2,
                name = "Signed in, still locked out",
                problemStatement = "An accepted authentication session is rejected by the next service.",
                postCount = 24,
                isWatched = true,
                category = OpportunityCategory.DATA_AND_APIS,
            ),
        ),
        OpportunityCardPreviewCase(
            build.copy(
                id = 3,
                name = "Automation needs manual recovery",
                problemStatement = "A background job fails silently and someone has to check every run.",
                category = OpportunityCategory.WORKFLOW_AND_AUTOMATION,
            ),
        ),
        OpportunityCardPreviewCase(build, pending = true),
        OpportunityCardPreviewCase(build.copy(isWatched = true), pending = true),
        OpportunityCardPreviewCase(
            build.copy(
                id = 4,
                name = "Unclassified opportunity",
                summary = "A complete opportunity summary for the preview.",
                problemStatement = "Teams need a reliable way to complete this task.",
                postCount = 0,
                date = null,
                category = OpportunityCategory.UNCATEGORIZED,
            ),
        ),
        OpportunityCardPreviewCase(
            build.copy(
                id = 5,
                name = "A particularly long opportunity title that must fit into two lines without moving the footer",
                problemStatement = "Teams need a reliable way to complete this task.",
                summary = "This long summary exercises the description line limit. The complete explanation belongs on the details screen rather than expanding this card.",
                postCount = 1,
            ),
        ),
    )
}
