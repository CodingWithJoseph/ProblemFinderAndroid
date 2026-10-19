package com.example.problemfinder.feature.opportunities.impl.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.problemfinder.core.designsystem.theme.DiscoveryTypography
import com.example.problemfinder.core.designsystem.theme.appearance
import com.example.problemfinder.core.designsystem.theme.featurePalette
import com.example.problemfinder.core.model.opportunity.Opportunity
import com.example.problemfinder.feature.opportunities.impl.R
import com.example.problemfinder.feature.opportunities.impl.discovery.DiscoveryIntent
import com.example.problemfinder.feature.opportunities.impl.util.toOpportunityDateLabelCompact
import java.time.ZoneId
import kotlin.time.Instant

@Composable
internal fun FeaturedDiscoveryCard(
    cluster: Opportunity,
    isActionPending: Boolean,
    onIntent: (DiscoveryIntent) -> Unit,
    modifier: Modifier = Modifier,
) = Card(
    onClick = { onIntent(DiscoveryIntent.OpenCluster(cluster.id)) },
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(
        containerColor = featurePalette().surface,
        contentColor = featurePalette().foreground,
    ),
    modifier = modifier
        .fillMaxWidth()
        .heightIn(min = 338.dp)
) {
    Box(Modifier.fillMaxWidth()) {
        FeaturedCardDecoration(
            iconRes = cluster.category.appearance().icon,
            modifier = Modifier.matchParentSize(),
        )

        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 338.dp)
                .padding(24.dp),
        ) {

            Column {
                FeaturedDiscoveryCardEyebrow(modifier = Modifier.fillMaxWidth())

                Spacer(Modifier.height(48.dp))

                FeaturedDiscoveryCardContent(cluster)

                Spacer(modifier = Modifier.height(24.dp))


            }
            FeaturedDiscoveryCardFooter(
                cluster = cluster,
                isActionPending = isActionPending,
                onWatchAction = { onIntent(DiscoveryIntent.WatchCluster(cluster.id)) }
            )
        }
    }
}


@Composable
private fun FeaturedDiscoveryCardEyebrow(
    modifier: Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.label_latest_discovery).uppercase(LocalLocale.current.platformLocale),
            style = DiscoveryTypography.featuredBadge,
            color = featurePalette().actionContainer,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .border(
                    border = BorderStroke(
                        width = 1.dp,
                        color = featurePalette().border
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(vertical = 5.dp, horizontal = 9.dp)
        )
    }

}

@Composable
private fun FeaturedDiscoveryCardContent(cluster: Opportunity) =
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        val title = cluster.name
        val description = cluster.problemStatement
        Text(
            text = title,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            style = DiscoveryTypography.featuredTitle,
            color = featurePalette().foreground,
            modifier = Modifier.widthIn(max = 260.dp)
        )

        Text(
            text = description,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = DiscoveryTypography.featuredDescription,
            color = featurePalette().secondaryText,
            modifier = Modifier
        )
    }

@Composable
private fun FeaturedDiscoveryCardFooter(
    cluster: Opportunity,
    isActionPending: Boolean,
    onWatchAction: () -> Unit,
) = Row(
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.fillMaxWidth()
) {

    FeatureDiscoveryMetaData(
        postCount = cluster.postCount,
        date = cluster.date,
        modifier = Modifier.weight(1f)
    )

    AddRemoveButton(
        cluster = cluster,
        isActionPending = isActionPending,
        palette = featurePalette(),
        onWatchAction = onWatchAction
    )
}

@Composable
fun FeatureDiscoveryMetaData(
    postCount: Int,
    date: Instant?,
    modifier: Modifier
) = Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    modifier = modifier
) {
    val metadataColor = featurePalette().secondaryText
    Text(
        text = pluralStringResource(
            id = R.plurals.browse_post_count,
            count = postCount,
            postCount
        ),
        maxLines = 1,
        style = DiscoveryTypography.metadata,
        color = metadataColor
    )

    Canvas(modifier = Modifier.size(width = 4.dp, height = 4.dp)) {
        drawOval(
            color = metadataColor,
            topLeft = Offset.Zero,
            size = size
        )
    }

    val locale = LocalConfiguration.current.locales[0]
    val zone = ZoneId.systemDefault()
    val dateLabel = remember(date, locale, zone) {
        date?.toOpportunityDateLabelCompact(locale, zone)
    }

    Text(
        text = dateLabel ?: stringResource(R.string.label_activity_date_unavailable),
        maxLines = 1,
        style = DiscoveryTypography.metadata,
        color = metadataColor
    )
}
