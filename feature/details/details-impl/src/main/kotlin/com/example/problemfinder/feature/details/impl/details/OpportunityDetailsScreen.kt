package com.example.problemfinder.feature.details.impl.details

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.problemfinder.core.designsystem.components.*
import com.example.problemfinder.core.designsystem.theme.appearance
import com.example.problemfinder.core.designsystem.theme.opportunityPalette
import com.example.problemfinder.core.mvi.RequestState
import com.example.problemfinder.feature.details.impl.R
import com.example.problemfinder.feature.details.impl.util.messageRes
import com.example.problemfinder.core.designsystem.R as DesignR

enum class DetailsSection { OVERVIEW, EVIDENCE }

/** Renders supplied state. Section selection is navigation; request handling stays in the VM. */
@Composable
internal fun OpportunityDetailsScreen(
    state: OpportunityDetailsState,
    onIntent: (OpportunityDetailsIntent) -> Unit,
    modifier: Modifier = Modifier,
    section: DetailsSection = DetailsSection.OVERVIEW,
    onOverview: () -> Unit = {},
) {
    val details = state.details
    val appearance = state.cachedOpportunity?.category?.appearance(isSystemInDarkTheme())
    val refreshError = (state.refreshRequest as? RequestState.Error)?.error
    val observationError = (state.observationRequest as? RequestState.Error)?.error
    val contentError = refreshError ?: observationError
    FeatureScaffold(
        stringResource(R.string.title_opportunity_details), modifier,
        onBack = { onIntent(OpportunityDetailsIntent.BackClicked) },
        onRefresh = { onIntent(OpportunityDetailsIntent.Reload) },
        refreshing = state.refreshRequest is RequestState.Loading,
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            if (details == null) item("status") {
                when {
                    observationError != null || refreshError != null -> {
                        val error = observationError ?: refreshError!!
                        FeatureEmpty(stringResource(error.messageRes().first), stringResource(error.messageRes().second),
                            DesignR.drawable.icon_warning, stringResource(DesignR.string.ui_retry),
                            { onIntent(OpportunityDetailsIntent.Reload) })
                    }
                    state.observationRequest is RequestState.Loading ||
                        state.refreshRequest is RequestState.Loading || !state.cacheReady ->
                        FeatureLoading(stringResource(R.string.details_loading))
                    else -> FeatureEmpty(stringResource(R.string.details_empty_title),
                        stringResource(R.string.details_empty_message), DesignR.drawable.icon_document,
                        stringResource(DesignR.string.ui_retry), { onIntent(OpportunityDetailsIntent.Reload) })
                }
            } else {
                if (contentError != null) item("cached-error") {
                    FeatureNotice(stringResource(R.string.details_saved_title), stringResource(contentError.messageRes().second),
                        { onIntent(OpportunityDetailsIntent.Reload) }, DesignR.drawable.icon_wifi_off)
                }
                item("hero") {
                    FeatureHero(appearance?.let { stringResource(it.title) } ?: stringResource(R.string.details_eyebrow),
                        details.name, details.summary, icon = appearance?.icon ?: DesignR.drawable.icon_uncategorized) {
                        details.lastSeen?.let { Text(stringResource(R.string.details_latest_activity,
                            it.toString().substringBefore('T')), style = MaterialTheme.typography.bodySmall) }
                        FeatureAction(
                            stringResource(if (state.cachedOpportunity?.isWatched == true) R.string.details_remove else R.string.details_add),
                            { onIntent(OpportunityDetailsIntent.WatchClicked) },
                            icon = if (state.cachedOpportunity?.isWatched == true) DesignR.drawable.icon_remove else DesignR.drawable.icon_add,
                            prominent = true, destructive = state.cachedOpportunity?.isWatched == true,
                            pending = state.isWatchPending,
                            enabled = state.cachedOpportunity != null && !state.implementationPending,
                        )
                        if (state.cachedOpportunity == null) Text(stringResource(R.string.details_watch_unknown),
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
                item("sections") {
                    TabRow(
                        selectedTabIndex = section.ordinal,
                        containerColor = MaterialTheme.colorScheme.background,
                        contentColor = opportunityPalette().foreground,
                        indicator = { positions -> TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(positions[section.ordinal]),
                            color = opportunityPalette().foreground,
                        ) },
                    ) {
                        Tab(section == DetailsSection.OVERVIEW, onClick = onOverview,
                            text = { Text(stringResource(R.string.details_overview)) })
                        Tab(section == DetailsSection.EVIDENCE,
                            onClick = { onIntent(OpportunityDetailsIntent.OpenEvidenceList) },
                            text = { Text(stringResource(R.string.details_evidence_count, details.postCount)) })
                    }
                }
                if (section == DetailsSection.OVERVIEW) {
                    item("metrics") { FeatureMetrics(listOf(
                        details.postCount.toString() to stringResource(R.string.details_posts),
                        details.authorCount.toString() to stringResource(R.string.details_authors),
                        details.communityCount.toString() to stringResource(R.string.details_communities))) }
                    item("problem") { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        FeatureSection(stringResource(R.string.details_problem))
                        Text(details.problemStatement, style = MaterialTheme.typography.bodyLarge)
                    } }
                    item("top-heading") { FeatureSection(stringResource(R.string.details_top_evidence)) }
                    if (state.topEvidence.isEmpty()) item("no-top") {
                        FeatureEmpty(stringResource(R.string.details_no_evidence),
                            stringResource(R.string.details_no_evidence_message), DesignR.drawable.icon_message)
                    }
                    items(state.topEvidence, key = { "top-" + it.id }) { post ->
                        EvidenceListItem(post, { onIntent(OpportunityDetailsIntent.OpenEvidence(post.id)) }, highlighted = true)
                    }
                    item("all") { OutlinedButton(
                        { onIntent(OpportunityDetailsIntent.OpenEvidenceList) }, Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) {
                        Text(stringResource(R.string.details_all_posts))
                        Spacer(Modifier.width(8.dp)); FeatureIcon(DesignR.drawable.icon_arrow_up_right)
                    } }
                    if (details.postVolumeByDate.isNotEmpty()) {
                        item("activity-heading") { FeatureSection(stringResource(R.string.details_activity)) }
                        items(details.postVolumeByDate, key = { "date-" + it.date }) { point ->
                            ListItem(headlineContent = { Text(point.date) },
                                trailingContent = { Text(pluralStringResource(DesignR.plurals.ui_posts, point.count, point.count)) })
                        }
                    }
                    if (details.sourceBreakdown.isNotEmpty()) {
                        item("source-heading") { FeatureSection(stringResource(R.string.details_sources)) }
                        items(details.sourceBreakdown, key = { "source-" + it.source }) { source ->
                            ListItem(headlineContent = { Text(source.source) },
                                leadingContent = { FeatureIcon(DesignR.drawable.icon_message) },
                                trailingContent = { Text(source.count.toString()) })
                        }
                    }
                    item("dates") { FeaturePanel {
                        details.firstSeen?.let { Text(stringResource(R.string.details_first_seen,
                            it.toString().substringBefore('T')), style = MaterialTheme.typography.bodySmall) }
                        details.lastSeen?.let { Text(stringResource(R.string.details_latest_activity,
                            it.toString().substringBefore('T')), style = MaterialTheme.typography.bodySmall) }
                    } }
                } else {
                    item("posts-heading") { FeatureSection(stringResource(R.string.details_supporting_posts)) }
                    if (state.posts.isEmpty()) item("no-posts") {
                        FeatureEmpty(stringResource(R.string.details_no_evidence),
                            stringResource(R.string.details_no_evidence_message), DesignR.drawable.icon_message)
                    }
                    items(state.posts, key = { "post-" + it.id }) { post ->
                        EvidenceListItem(post, { onIntent(OpportunityDetailsIntent.OpenEvidence(post.id)) })
                    }
                    item("paging") {
                        when (val request = state.pageRequest) {
                            is RequestState.Loading -> FeatureLoading(stringResource(R.string.details_loading_posts))
                            is RequestState.Error -> FeatureNotice(stringResource(R.string.details_page_error),
                                stringResource(request.error.messageRes().second), { onIntent(OpportunityDetailsIntent.RetryEvidencePage) })
                            else -> if (state.canLoadMore) FeatureAction(stringResource(R.string.details_load_more),
                                { onIntent(OpportunityDetailsIntent.LoadNextEvidencePage) },
                                Modifier.fillMaxWidth(), DesignR.drawable.icon_add)
                            else if (state.posts.isNotEmpty()) Text(stringResource(R.string.details_all_loaded),
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
