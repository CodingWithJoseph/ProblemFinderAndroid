package com.example.problemfinder.feature.opportunities.impl.discovery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.problemfinder.core.designsystem.theme.DiscoveryTypography
import com.example.problemfinder.core.designsystem.theme.opportunityPalette
import com.example.problemfinder.core.model.opportunity.OpportunityCategory
import com.example.problemfinder.core.model.opportunity.Opportunity
import com.example.problemfinder.core.mvi.RequestState
import com.example.problemfinder.feature.opportunities.impl.R
import com.example.problemfinder.feature.opportunities.impl.components.OpportunityRefreshAction
import com.example.problemfinder.feature.opportunities.impl.components.DiscoveryCollectionRow
import com.example.problemfinder.feature.opportunities.impl.components.FeaturedDiscoveryCard
import com.example.problemfinder.feature.opportunities.impl.util.messageRes

@Composable
internal fun DiscoveryScreen(
    state: DiscoveryState,
    onIntent: (DiscoveryIntent) -> Unit,
    modifier: Modifier = Modifier,
) = Surface(
    modifier = modifier.fillMaxSize(),
    color = MaterialTheme.colorScheme.background,
) {

    val featuredCluster = state.featuredCluster
    val collections = state.collections
    val pendingClusterIds = state.pendingClusterIds

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 24.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(30.dp),
    ) {
        item(key = "introduction") {
            DiscoveryDashboardHeader(
                canRefresh = state.canRefresh, isRefreshing = state.isRefreshing,
                onRefresh = { onIntent(DiscoveryIntent.Reload) },
            )
        }

        if (state.contentError != null || !state.hasContent || state.isRefreshing) {
            item(key = "discovery-status") {
                DiscoveryStatus(state = state)
            }
        }

        if (featuredCluster != null) {
            item(key = "featured") {
                FeaturedDiscoveryCard(
                    cluster = featuredCluster,
                    isActionPending = featuredCluster.id in pendingClusterIds,
                    onIntent=onIntent,
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
            }
        }

        items(collections, key = { it.category.name }) { collection ->
            DiscoveryCollectionRow(
                category = collection.category,
                clusters = collection.clusters,
                pendingClusterIds = pendingClusterIds,
                onIntent = onIntent
            )
        }
    }
}

@Composable
private fun DiscoveryStatus(state: DiscoveryState) {
    val error = state.contentError

    val loading = error == null && (
        !state.cacheReady ||
            state.observationRequest == RequestState.Loading ||
            state.isRefreshing
        )

    val messageRes = when {
        error != null -> error.messageRes().second
        loading -> if (state.hasContent) {
            R.string.message_refreshing
        } else {
            R.string.message_no_data_loading
        }
        state.userId == null -> R.string.message_error_session
        !state.hasContent && state.hasSynced ->
            R.string.message_no_data_succeeded
        !state.hasContent -> R.string.message_not_synced
        else -> null
    }

    Column(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (messageRes != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp)
                            .clearAndSetSemantics {},
                        strokeWidth = 2.dp,
                    )
                }

                Text(
                    text = stringResource(messageRes),
                    modifier = Modifier.weight(1f).semantics {
                        liveRegion = LiveRegionMode.Polite
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (error != null) {
                        MaterialTheme.colorScheme.error
                    } else {
                        opportunityPalette().secondaryText
                    },
                )
            }
        }


    }
}

@Composable
internal fun DiscoveryDashboardHeader(
    modifier: Modifier = Modifier,
    canRefresh: Boolean = false,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
) {
    val palette = opportunityPalette()
    val locale = LocalConfiguration.current.locales[0]
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.label_discovery_introduction).uppercase(locale),
                style = DiscoveryTypography.dashboardEyebrow,
                color = palette.secondaryText,
                modifier = Modifier.weight(1f),
            )
            OpportunityRefreshAction(enabled = canRefresh, loading = isRefreshing, onClick = onRefresh)
        }
        DiscoveryDashboardHeadline()
        Text(
            text = stringResource(R.string.description_discovery_dashboard),
            style = MaterialTheme.typography.bodyMedium,
            color = palette.secondaryText,
        )
    }
}

@Composable
internal fun DiscoveryDashboardHeadline(modifier: Modifier = Modifier) = Text(
    text = stringResource(R.string.title_discovery_dashboard),
    style = DiscoveryTypography.dashboardTitle,
    color = opportunityPalette().foreground,
    modifier = modifier.semantics { heading() },
)
