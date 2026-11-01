package com.example.problemfinder.feature.opportunitydetails.impl.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.problemfinder.core.designsystem.theme.DiscoverySpotlightColors
import com.example.problemfinder.core.designsystem.R as DesignSystemR
import com.example.problemfinder.core.designsystem.theme.DiscoveryTypography
import com.example.problemfinder.core.designsystem.theme.OpportunityPalette
import com.example.problemfinder.core.designsystem.theme.appearance
import com.example.problemfinder.core.designsystem.theme.featurePalette
import com.example.problemfinder.core.model.opportunity.Opportunity
import com.example.problemfinder.feature.opportunitydetails.impl.details.OpportunityDetailsIntent
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.time.Instant

@Composable
internal fun FeaturedDiscoveryCard(
    cluster: Opportunity,
    isActionPending: Boolean,
    onIntent: (OpportunityDetailsIntent) -> Unit,
    modifier: Modifier = Modifier,
) = Card(
    onClick = { },
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
                onWatchAction = { onIntent(OpportunityDetailsIntent.WatchClicked) }
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
            text = stringResource(DesignSystemR.string.label_latest_discovery).uppercase(LocalLocale.current.platformLocale),
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
            id = DesignSystemR.plurals.browse_post_count,
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
        text = dateLabel ?: stringResource(DesignSystemR.string.label_activity_date_unavailable),
        maxLines = 1,
        style = DiscoveryTypography.metadata,
        color = metadataColor
    )
}


internal fun Instant.toOpportunityDateLabelCompact(locale: Locale, zone: ZoneId): String {
    val javaInstant = java.time.Instant.ofEpochSecond(
        epochSeconds,
        nanosecondsOfSecond.toLong(),
    )
    return DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        .withLocale(locale)
        .format(javaInstant.atZone(zone))
        .substringBefore(',')
}


@Composable
internal fun AddRemoveButton(
    cluster: Opportunity,
    isActionPending: Boolean,
    palette: OpportunityPalette,
    onWatchAction: () -> Unit,
) {
    val container = if (cluster.isWatched) palette.removeContainer else palette.actionContainer
    val content = if (cluster.isWatched) palette.removeContent else palette.actionContent
    val action = stringResource(if (cluster.isWatched) DesignSystemR.string.action_remove_opportunity else DesignSystemR.string.action_add_opportunity, cluster.name)
    val pending = stringResource(if (cluster.isWatched) DesignSystemR.string.message_removing_opportunity else DesignSystemR.string.message_adding_opportunity)
    FilledIconButton(
        onClick = onWatchAction,
        enabled = !isActionPending,
        shape = RoundedCornerShape(14.dp),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = container, contentColor = content,
            disabledContainerColor = container.copy(alpha = 0.6f), disabledContentColor = content.copy(alpha = 0.6f),
        ),
        modifier = Modifier.size(48.dp).semantics {
            contentDescription = action
            if (isActionPending) stateDescription = pending
        },
    ) {
        if (isActionPending) CircularProgressIndicator(Modifier.size(16.dp).clearAndSetSemantics {}, color = LocalContentColor.current, strokeWidth = 2.dp)
        else Icon(
            painterResource(if (cluster.isWatched) DesignSystemR.drawable.icon_remove else DesignSystemR.drawable.icon_add),
            contentDescription = null, modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
internal fun FeaturedCardDecoration(
    @DrawableRes iconRes: Int,
    modifier: Modifier = Modifier,
) {
    val iconTileShape = RoundedCornerShape(19.dp)

    Box(modifier = modifier.clearAndSetSemantics {}) {
        Canvas(Modifier.matchParentSize()) {
            val center = Offset(size.width - 40.dp.toPx(), 65.dp.toPx())
            val stroke = Stroke(width = 1.dp.toPx())
            for (radius in listOf(120.dp, 85.dp, 50.dp)) {
                drawCircle(
                    color = DiscoverySpotlightColors.ring,
                    radius = radius.toPx(),
                    center = center,
                    style = stroke,
                )
            }
        }

        Box(
            modifier = Modifier.align(Alignment.TopEnd)
                .padding(top = 33.dp, end = 48.dp)
                .size(67.dp)
                .rotate(-12f)
                .shadow(8.dp, iconTileShape, clip = false)
                .background(DiscoverySpotlightColors.accent, iconTileShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = DiscoverySpotlightColors.surface,
                modifier = Modifier.size(32.dp),
            )
        }
    }
}

