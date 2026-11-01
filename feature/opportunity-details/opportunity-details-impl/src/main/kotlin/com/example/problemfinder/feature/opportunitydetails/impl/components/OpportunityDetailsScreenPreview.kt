package com.example.problemfinder.feature.opportunitydetails.impl.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.problemfinder.core.designsystem.theme.DiscoveryTypography
import com.example.problemfinder.core.designsystem.theme.ImmensityTheme
import com.example.problemfinder.core.designsystem.theme.opportunityPalette

private val DashboardFeaturedExample = FeaturedCardPreviewProvider().values.first()

@Preview(name = "Phone - light", widthDp = 393, heightDp = 852, showBackground = true)
@Preview(name = "Phone - dark", widthDp = 393, heightDp = 852, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Full dashboard - light", widthDp = 393, heightDp = 1900, showBackground = true)
@Preview(name = "Full dashboard - dark", widthDp = 393, heightDp = 1900, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Narrow phone", widthDp = 320, heightDp = 852, showBackground = true)
@Preview(name = "Large text", widthDp = 393, heightDp = 852, showBackground = true, fontScale = 1.5f)
@Preview(name = "Double text size", widthDp = 393, heightDp = 852, showBackground = true, fontScale = 2f)
@Composable
private fun DiscoveryDashboardPreview() = DiscoveryDashboardPreviewReference()

@Preview(name = "Featured - watching", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
private fun DiscoveryDashboardWatchingPreview() = DiscoveryDashboardPreviewReference(
    featured = DashboardFeaturedExample.copy(
        cluster = DashboardFeaturedExample.cluster.copy(isWatched = true),
    ),
)

@Preview(name = "Featured - adding", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
private fun DiscoveryDashboardAddingPreview() = DiscoveryDashboardPreviewReference(
    featured = DashboardFeaturedExample.copy(pending = true),
)

@Composable
private fun DiscoveryDashboardPreviewReference(
    featured: OpportunityCardPreviewCase = DashboardFeaturedExample,
) = ImmensityTheme {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(30.dp),
        ) {
            item(key = "featured") {
                FeaturedDiscoveryCard(
                    cluster = featured.cluster,
                    isActionPending = featured.pending,
                    onIntent = {},
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
            }

//            items(DashboardCollectionExamples, key = { it.category.name }) { collection ->
//                DiscoveryCollectionRow(
//                    category = collection.category,
//                    clusters = collection.clusters,
//                    pendingClusterIds = collection.pendingClusterIds,
//                    onIntent = {},
//                )
//            }
        }
    }
}


@Composable
private fun DiscoveryDashboardIntroductionPreview() {
    val palette = opportunityPalette()
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "OPPORTUNITY STARTS HERE",
            style = DiscoveryTypography.featuredBadge.copy(
                fontSize = 11.sp,
                letterSpacing = 1.8.sp,
            ),
            color = palette.secondaryText,
        )
        Text(
            text = "Discover the\nnext possibility.",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 42.sp,
                lineHeight = 45.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-2).sp,
            ),
            color = palette.foreground,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = "Real problems. A place to start.",
            style = MaterialTheme.typography.bodyMedium,
            color = palette.secondaryText,
        )
    }
}
