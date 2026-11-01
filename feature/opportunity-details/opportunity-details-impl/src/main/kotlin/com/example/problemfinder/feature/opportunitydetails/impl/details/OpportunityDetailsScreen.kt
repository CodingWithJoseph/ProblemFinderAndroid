package com.example.problemfinder.feature.opportunitydetails.impl.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.problemfinder.core.designsystem.theme.ImmensityTheme
import com.example.problemfinder.feature.opportunitydetails.impl.R
import com.example.problemfinder.feature.opportunitydetails.impl.components.FeaturedDiscoveryCard
import com.example.problemfinder.feature.opportunitydetails.impl.components.OpportunityCardPreviewCase
import com.example.problemfinder.core.designsystem.R as DesignR

/** State and intent wiring are supplied; the full text/evidence layout belongs to DETAILS-6. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OpportunityDetailsScreen(
    state: OpportunityDetailsState,
    onIntent: (OpportunityDetailsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                title = { Text(stringResource(R.string.title_opportunity_details)) },
                navigationIcon = {
                    IconButton(onClick = { onIntent(OpportunityDetailsIntent.BackClicked) }) {
                        Icon(painterResource(DesignR.drawable.icon_arrow_back), stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(
                        enabled = !state.implementationPending,
                        onClick = { onIntent(OpportunityDetailsIntent.Reload) },
                    ) {
                        Icon(painterResource(DesignR.drawable.icon_refresh), stringResource(R.string.action_refresh_details))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // TODO [DETAILS-6] Replace this shell with branches for initial loading, cached content,
            // refresh failure with content, missing/not-found data, and retry. Never initiate work
            // during composition; send intents from callbacks. Use resources for all user copy.
            state.clusterId?.let { Text(stringResource(R.string.label_opportunity_id, it)) }
            if (state.implementationPending) Text(stringResource(R.string.message_details_scaffold))
            state.details?.let { detail ->
                Text(detail.name, style = MaterialTheme.typography.headlineSmall)
                Text(detail.summary)
                Text(detail.problemStatement)
            }

            val cluster = state.cachedOpportunity

            cluster?.let {
                FeaturedDiscoveryCard(
                    cluster = it,
                    onIntent = onIntent,
                    isActionPending = false,
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
            }
        }
    }
}

@Preview(name = "Details scaffold", widthDp = 393, heightDp = 850)
@Preview(name = "Details scaffold large text", widthDp = 320, heightDp = 640, fontScale = 2f)
@Composable
private fun OpportunityDetailsPreview() = ImmensityTheme {
    OpportunityDetailsScreen(
        OpportunityDetailsState(
            clusterId = 42
        ), {

        }
    )
    // TODO [DETAILS-6] Add preview fixtures for full content, no evidence, missing post URLs,
    // loading, offline refresh failure and append failure. Fixtures stay in previews/tests only.
}
