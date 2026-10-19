package com.example.problemfinder.feature.opportunities.impl.browse

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.problemfinder.core.designsystem.theme.ImmensityTheme
import com.example.problemfinder.core.model.opportunity.OpportunityFailure
import com.example.problemfinder.core.model.opportunity.Opportunity
import com.example.problemfinder.core.mvi.RequestState
import com.example.problemfinder.feature.opportunities.impl.R
import com.example.problemfinder.feature.opportunities.impl.components.OpportunityRefreshAction
import com.example.problemfinder.feature.opportunities.impl.components.BrowseOpportunityRow
import com.example.problemfinder.feature.opportunities.impl.util.OpportunityUiError
import com.example.problemfinder.feature.opportunities.impl.util.messageRes
import kotlin.time.Instant

/** Renders cached results and independent request states; all user actions return as intents. */
@Composable
fun BrowseScreen(
    state: BrowseState,
    modifier: Modifier = Modifier,
    onIntent: (BrowseIntent) -> Unit,
) = Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(state.category.titleRes()),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(stringResource(state.sort.titleRes()), style = MaterialTheme.typography.bodySmall)
            }
            OpportunityRefreshAction(
                enabled = state.canRefresh,
                loading = state.refreshRequest == RequestState.Loading,
                onClick = { onIntent(BrowseIntent.Reload) },
            )
            BrowseFilterMenu(state, onIntent)
        }

        val contentError = state.contentError
        // A failed refresh/observation leaves existing rows useful; show an inline retry banner.
        if (contentError != null && state.data.isNotEmpty()) {
            BrowseBanner(
                message = stringResource(contentError.messageRes().second),
                buttonTitle = stringResource(R.string.title_retry_refresh_button),
                enabled = state.canRefresh,
                onAction = { onIntent(BrowseIntent.Reload) },
            )
        }

        if (state.data.isEmpty()) {
            BrowseNoDataScreen(state, Modifier.weight(1f), onIntent)
        } else {
            BrowseDataScreen(state, Modifier.weight(1f), onIntent)
        }
    }
}

@Composable
private fun BrowseNoDataScreen(
    state: BrowseState,
    modifier: Modifier,
    onIntent: (BrowseIntent) -> Unit,
) {
    val error = state.contentError
    val loading = error == null && (
        !state.cacheReady || state.refreshRequest == RequestState.Loading
        )
    // Only a synced cache establishes a successful empty result; a first read may still need sync.
    val resources = when {
        error != null -> error.messageRes()
        loading -> R.string.title_no_data_loading to R.string.message_no_data_loading
        state.userId == null -> R.string.title_failure to R.string.message_error_session
        state.hasSynced -> R.string.title_no_data_succeeded to R.string.message_no_data_succeeded
        else -> R.string.title_not_synced to R.string.message_not_synced
    }

    BoxWithConstraints(modifier.fillMaxWidth()) {
        val viewportHeight = maxHeight
        Column(
            modifier = Modifier.fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = viewportHeight)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(resources.first),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(resources.second),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            if (loading) {
                CircularProgressIndicator(Modifier.clearAndSetSemantics {})
            }
            if (state.cursor != null) {
                BrowsePageFooter(state, onIntent)
            }
        }
    }
}

@Composable
private fun BrowseDataScreen(
    state: BrowseState,
    modifier: Modifier,
    onIntent: (BrowseIntent) -> Unit,
) {
    val listState = rememberLazyListState()
    // Request ahead of the final row; derivedStateOf avoids reacting to every scroll pixel.
    val reachedBottom by remember(listState) {
        derivedStateOf {
            val layout = listState.layoutInfo
            val lastVisible = layout.visibleItemsInfo.lastOrNull()?.index
            layout.totalItemsCount > 0 && lastVisible != null &&
                lastVisible >= layout.totalItemsCount - 1 - 4
        }
    }

    // A new cursor can trigger the next page while the viewport is still near the bottom.
    // The ViewModel also deduplicates cursors and requires explicit retry after page errors.
    LaunchedEffect(reachedBottom, state.cursor, state.canAutoLoadNextPage) {
        if (reachedBottom && state.canAutoLoadNextPage) {
            onIntent(BrowseIntent.LoadNextPage)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(state.data, key = { it.id }) { cluster ->
            BrowseOpportunityRow(
                cluster = cluster,
                onOpen = { onIntent(BrowseIntent.OpenCluster(cluster.id)) },
                isActionPending = cluster.id in state.pendingClusterIds,
                onWatchAction = { onIntent(BrowseIntent.WatchCluster(cluster.id)) },
            )
        }
        item(key = "browse-pagination-footer") {
            BrowsePageFooter(state, onIntent)
        }
    }
}

/** Keeps pagination errors local to the footer and provides a manual alternative to auto-loading. */
@Composable
private fun BrowsePageFooter(state: BrowseState, onIntent: (BrowseIntent) -> Unit) {
    when (val request = state.pageRequest) {
        RequestState.Loading -> Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp)
                .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator(Modifier.size(20.dp).clearAndSetSemantics {}, strokeWidth = 2.dp)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.message_loading_more), textAlign = TextAlign.Center)
        }
        is RequestState.Error -> BrowseBanner(
            message = stringResource(request.error.messageRes().second),
            buttonTitle = stringResource(R.string.title_retry_page_button),
            enabled = state.canLoadNextPage,
            onAction = { onIntent(BrowseIntent.RetryNextPage) },
        )
        else -> {
            if (state.cursor != null) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    TextButton(
                        enabled = state.canLoadNextPage,
                        onClick = { onIntent(BrowseIntent.RetryNextPage) },
                    ) {
                        Text(stringResource(R.string.title_load_more_button))
                    }
                }
            } else if (state.hasSynced) {
                Text(
                    stringResource(R.string.message_end_of_results),
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun BrowseBanner(
    message: String,
    buttonTitle: String,
    enabled: Boolean,
    onAction: () -> Unit,
) = Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
    Text(
        message,
        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
    )
    OpportunityRefreshAction(modifier = Modifier.align(Alignment.End), enabled = enabled, onClick = onAction, label = buttonTitle)
}

@Preview(name = "Compact", widthDp = 360, heightDp = 640, showBackground = true)
@Preview(name = "Tall", widthDp = 393, heightDp = 850, showBackground = true)
@Preview(name = "Landscape", widthDp = 640, heightDp = 360, showBackground = true)
@Preview(name = "Dark", widthDp = 360, heightDp = 640, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Large text", widthDp = 360, heightDp = 640, fontScale = 2f)
@Composable
private fun BrowseScreenPreview() = ImmensityTheme {
    BrowseScreen(
        BrowseState(
            userId = "preview",
            data = listOf(
                Opportunity(
                    id = 1,
                    name = "Maintenance request tracking",
                    summary = "Property teams struggle to track requests through completion.",
                    problemStatement = "Teams need a reliable way to complete this task.",
                    postCount = 23,
                    isWatched = true,
                    date = Instant.parse("2026-10-10T12:00:00Z"),
                ),
                Opportunity(
                    id = 2,
                    name = "Unclassified opportunity",
                    summary = "A complete opportunity summary for the preview.",
                    problemStatement = "How can teams keep guest requests from getting lost between shifts?",
                    postCount = 1,
                    isWatched = false,
                    date = null,
                ),
                Opportunity(
                    id = 3,
                    name = "Helping small businesses plan inventory when demand changes unexpectedly",
                    summary = "Owners need to spot shortages early and coordinate purchasing decisions across a busy team.",
                    problemStatement = "Teams need a reliable way to complete this task.",
                    postCount = 0,
                    isWatched = false,
                    date = Instant.parse("2026-10-09T01:00:00Z"),
                ),
            ),
            hasSynced = true,
            cacheReady = true,
            observationRequest = RequestState.Success(Unit),
        ),
    ) {}
}

@Preview(name = "Empty error, large landscape", widthDp = 640, heightDp = 360, fontScale = 2f)
@Composable
private fun BrowseErrorPreview() = ImmensityTheme {
    BrowseScreen(
        BrowseState(
            userId = "preview",
            cacheReady = true,
            observationRequest = RequestState.Success(Unit),
            refreshRequest = RequestState.Error(OpportunityUiError(OpportunityFailure.NETWORK)),
        ),
    ) {}
}
