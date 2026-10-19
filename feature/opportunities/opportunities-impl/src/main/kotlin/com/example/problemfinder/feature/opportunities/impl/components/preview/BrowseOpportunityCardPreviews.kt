package com.example.problemfinder.feature.opportunities.impl.components.preview

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.example.problemfinder.core.designsystem.theme.ImmensityTheme
import com.example.problemfinder.feature.opportunities.impl.components.BrowseOpportunityRow

@Preview(name = "Compact · light", widthDp = 360, showBackground = true)
@Preview(name = "Compact · dark", widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Compact · double text", widthDp = 320, fontScale = 2f)
@Composable
private fun BrowseOpportunityCardPreview(
    @PreviewParameter(OpportunityCardPreviewProvider::class) case: OpportunityCardPreviewCase,
) = ImmensityTheme {
    Surface(color = MaterialTheme.colorScheme.background) {
        BrowseOpportunityRow(
            cluster = case.cluster,
            isActionPending = case.pending,
            onOpen = {},
            onWatchAction = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(name = "See all · compact list", widthDp = 393, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BrowseCompactListPreview() = ImmensityTheme {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OpportunityCardPreviewProvider().values.take(3).forEach { case ->
                BrowseOpportunityRow(
                    cluster = case.cluster,
                    isActionPending = case.pending,
                    onOpen = {},
                    onWatchAction = {},
                )
            }
        }
    }
}
