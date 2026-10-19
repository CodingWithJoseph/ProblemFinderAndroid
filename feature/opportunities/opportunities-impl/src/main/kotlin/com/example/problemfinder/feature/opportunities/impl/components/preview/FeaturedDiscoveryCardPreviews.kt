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
import com.example.problemfinder.feature.opportunities.impl.components.FeaturedDiscoveryCard

internal class FeaturedCardPreviewProvider : PreviewParameterProvider<OpportunityCardPreviewCase> {
    // Reuse the card's action, category, unclassified-category and missing-date, and overflow cases.
    // The featured build example uses the reference's editorial title in preview data only.
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
