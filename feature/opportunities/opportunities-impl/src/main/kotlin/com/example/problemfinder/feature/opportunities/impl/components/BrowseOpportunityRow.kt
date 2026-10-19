package com.example.problemfinder.feature.opportunities.impl.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.problemfinder.core.designsystem.theme.DiscoveryTypography
import com.example.problemfinder.core.designsystem.theme.appearance
import com.example.problemfinder.core.designsystem.theme.opportunityPalette
import com.example.problemfinder.core.model.opportunity.Opportunity
import com.example.problemfinder.feature.opportunities.impl.R
import com.example.problemfinder.feature.opportunities.impl.util.toOpportunityDateLabel
import java.time.ZoneId

/** Compact counterpart of OpportunityCard for the vertical See all results. */
@Composable
internal fun BrowseOpportunityRow(
    cluster: Opportunity,
    onOpen: () -> Unit,
    isActionPending: Boolean,
    onWatchAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = opportunityPalette()
    val category = cluster.category.appearance(darkTheme = isSystemInDarkTheme())
    val locale = LocalConfiguration.current.locales[0]
    val zone = ZoneId.systemDefault()
    val dateLabel = remember(cluster.date, locale, zone) {
        cluster.date?.toOpportunityDateLabel(locale, zone)
    }

    Card(
        onClick = onOpen,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = palette.surface,
            contentColor = palette.foreground,
        ),
        border = BorderStroke(1.dp, palette.border),
    ) {
        // Keep the category treatment without the dashboard card's tall illustration area.
        Row(
            modifier = Modifier.fillMaxWidth()
                .background(category.backgroundColor)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painterResource(category.icon),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = category.foregroundColor,
            )
            Text(
                text = stringResource(category.title).uppercase(locale),
                modifier = Modifier.weight(1f),
                style = DiscoveryTypography.categoryLabel,
                color = category.foregroundColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Content determines the height, so larger text never clips the action footer.
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = cluster.name,
                modifier = Modifier.semantics { heading() },
                style = DiscoveryTypography.cardTitle,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = cluster.problemStatement,
                style = DiscoveryTypography.description,
                color = palette.secondaryText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.browse_post_count, cluster.postCount, cluster.postCount,
                        ),
                        style = DiscoveryTypography.metadata,
                    )
                    Text(
                        text = dateLabel ?: stringResource(R.string.label_activity_date_unavailable),
                        style = DiscoveryTypography.activityDate,
                        color = palette.secondaryText,
                    )
                    if (cluster.isWatched) {
                        Text(
                            text = stringResource(R.string.label_watching),
                            style = DiscoveryTypography.metadata,
                            color = palette.secondaryText,
                        )
                    }
                }
                // This remains a separate click target from opening the card.
                AddRemoveButton(cluster, isActionPending, palette, onWatchAction)
            }
        }
    }
}
