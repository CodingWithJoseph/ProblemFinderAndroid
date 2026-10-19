package com.example.problemfinder.feature.opportunities.impl.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.problemfinder.core.designsystem.theme.appearance
import com.example.problemfinder.core.designsystem.theme.DiscoveryTypography
import com.example.problemfinder.core.designsystem.theme.opportunityPalette
import com.example.problemfinder.core.model.opportunity.OpportunityCategory
import com.example.problemfinder.core.model.opportunity.Opportunity
import com.example.problemfinder.feature.opportunities.impl.R
import com.example.problemfinder.feature.opportunities.impl.discovery.DiscoveryIntent
import com.example.problemfinder.core.designsystem.R as DesignSystemR

/** Horizontal category preview; See all opens Browse with the category, while cards open by ID. */
@Composable
internal fun DiscoveryCollectionRow(
    category: OpportunityCategory,
    clusters: List<Opportunity>,
    pendingClusterIds: Set<Int>,
    onIntent: (DiscoveryIntent) -> Unit,
    modifier: Modifier = Modifier,
) = Column(modifier = modifier.fillMaxWidth()) {
    val palette = opportunityPalette()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        val discoveryAppearance = category.appearance()
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = stringResource(discoveryAppearance.title),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() },
                style = DiscoveryTypography.collectionTitle,
                color = palette.foreground,
            )

            Text(
                text = stringResource(discoveryAppearance.description),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = DiscoveryTypography.subtitle,
                color = palette.secondaryText,
            )
        }

        Row(
            modifier = Modifier.heightIn(min = 48.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Button,
                    onClickLabel = stringResource(R.string.label_see_all_category, stringResource(discoveryAppearance.title)),
                    onClick = { onIntent(DiscoveryIntent.SeeAll(category)) },
                )
                .padding(start = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.label_see_all),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = DiscoveryTypography.seeAll,
                color = palette.foreground,
            )

            Icon(
                painter = painterResource(DesignSystemR.drawable.icon_arrow_up_right),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = palette.foreground,
            )
        }
    }

    Spacer(Modifier.height(12.dp))

    LazyRow(
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(clusters, key = {it.id}) {
            OpportunityCard(
                cluster = it,
                isActionPending = it.id in pendingClusterIds,
                onOpen = { onIntent(DiscoveryIntent.OpenCluster(it.id)) },
                onWatchAction = { onIntent(DiscoveryIntent.WatchCluster(it.id)) },
            )
        }
    }
}
