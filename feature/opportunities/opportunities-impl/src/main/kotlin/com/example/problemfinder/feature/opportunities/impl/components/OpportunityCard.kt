package com.example.problemfinder.feature.opportunities.impl.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.problemfinder.core.designsystem.theme.appearance
import com.example.problemfinder.core.designsystem.theme.DiscoveryTypography
import com.example.problemfinder.core.designsystem.theme.opportunityPalette
import com.example.problemfinder.core.model.opportunity.OpportunityCategory
import com.example.problemfinder.core.model.opportunity.Opportunity
import com.example.problemfinder.feature.opportunities.impl.R
import com.example.problemfinder.feature.opportunities.impl.util.toOpportunityDateLabel
import java.time.ZoneId


/** Fixed-size collection card with separate open/watch callbacks and no local watch-state ownership. */
@Composable
internal fun OpportunityCard(
    cluster: Opportunity,
    isActionPending: Boolean,
    onOpen: () -> Unit,
    onWatchAction: () -> Unit,
    modifier: Modifier = Modifier
) = Card(
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(
        containerColor = opportunityPalette().surface,
        contentColor = opportunityPalette().foreground,
    ),
    border = BorderStroke(1.dp, opportunityPalette().border),
    onClick = onOpen,
    modifier = modifier
        .width(290.dp)
        .height(320.dp)
) {

    OpportunityCardHeader(
        category = cluster.category,
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        OpportunityCardHeadline(
            cluster = cluster,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.weight(1f))

        OpportunityCardFooter(
            cluster = cluster,
            isActionPending = isActionPending,
            onWatchAction = onWatchAction,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}


@Composable
private fun OpportunityCardHeader(
    category: OpportunityCategory,
    modifier: Modifier
) {
    val appearance = category.appearance(darkTheme = isSystemInDarkTheme())
    val locale = LocalConfiguration.current.locales[0]
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(appearance.backgroundColor)
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .rotate(-8f)
                .border(
                    1.dp,
                    appearance.foregroundColor.copy(alpha = 0.6f),
                    RoundedCornerShape(15.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(appearance.icon),
                contentDescription = null,
                tint = appearance.foregroundColor,
                modifier = Modifier.size(24.dp),
            )
        }

        Spacer(Modifier.weight(1f))

        Text(
            text = stringResource(appearance.title).uppercase(locale),
            modifier = Modifier.widthIn(max = 94.dp),
            style = DiscoveryTypography.categoryLabel,
            color = appearance.foregroundColor,
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}


@Composable
private fun OpportunityCardHeadline(
    cluster: Opportunity,
    modifier: Modifier
) {
    val palette = opportunityPalette()
    // Preserve the fixed card height by shortening the preview at larger text sizes.
    val previewLines = if (LocalConfiguration.current.fontScale > 1.3f) 1 else 2
    val title = cluster.name
    val description = cluster.problemStatement

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        Text(
            text = title,
            modifier = Modifier.semantics { heading() },
            maxLines = previewLines,
            overflow = TextOverflow.Ellipsis,
            style = DiscoveryTypography.cardTitle,
            color = palette.foreground,
        )

        Text(
            text = description,
            maxLines = previewLines,
            overflow = TextOverflow.Ellipsis,
            style = DiscoveryTypography.description,
            color = palette.secondaryText,
        )
    }
}


@Composable
private fun OpportunityCardFooter(
    cluster: Opportunity,
    isActionPending: Boolean,
    onWatchAction: () -> Unit,
    modifier: Modifier
) {
    val palette = opportunityPalette()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        val locale = LocalConfiguration.current.locales[0]
        val zone = ZoneId.systemDefault()
        val dateLabel = remember(cluster.date, locale, zone) {
            cluster.date?.toOpportunityDateLabel(locale, zone)
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = pluralStringResource(
                    R.plurals.browse_post_count,
                    cluster.postCount,
                    cluster.postCount
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = DiscoveryTypography.metadata,
                color = palette.foreground,
            )

            Text(
                text = dateLabel ?: stringResource(R.string.label_activity_date_unavailable),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = DiscoveryTypography.activityDate,
                color = palette.secondaryText,
            )
        }

        AddRemoveButton(
            cluster = cluster,
            isActionPending = isActionPending,
            palette = opportunityPalette(),
            onWatchAction = onWatchAction
        )
    }
}
