package com.example.problemfinder.feature.search.impl

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.problemfinder.core.designsystem.components.*
import com.example.problemfinder.core.model.search.OpportunitySearchSort
import com.example.problemfinder.core.mvi.RequestState
import com.example.problemfinder.core.designsystem.R as DesignR

@Composable
fun SearchScreen(state: SearchState, onIntent: (SearchIntent) -> Unit, modifier: Modifier = Modifier) {
    FeatureScaffold(
        stringResource(R.string.title_search), modifier,
        onRefresh = { onIntent(SearchIntent.Reload) },
        refreshEnabled = state.canRefresh,
        refreshing = state.refreshRequest is RequestState.Loading,
        actions = {
            IconButton({ onIntent(SearchIntent.OpenFilters) }) {
                FeatureIcon(DesignR.drawable.icon_filter, description = stringResource(R.string.search_filters))
            }
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)) {
            item("intro") {
                FeaturePageHeader(stringResource(R.string.search_eyebrow), stringResource(R.string.search_headline),
                    stringResource(R.string.search_subtitle))
            }
            item("query") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        state.draftQuery.query, { onIntent(SearchIntent.QueryChanged(it)) },
                        Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.label_search_query)) },
                        leadingIcon = { FeatureIcon(DesignR.drawable.icon_search) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            onIntent(SearchIntent.Submit)
                        }),
                    )
                    state.validationMessage?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
                    FeatureAction(stringResource(R.string.action_search), { onIntent(SearchIntent.Submit) },
                        Modifier.fillMaxWidth(), DesignR.drawable.icon_search)
                    OutlinedButton({ onIntent(SearchIntent.OpenAssistant) }, Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        FeatureIcon(DesignR.drawable.icon_sparkles)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.search_assistant))
                    }
                }
            }
            if (state.submittedQuery != null) item("result-heading") {
                FeatureSection(stringResource(R.string.search_results),
                    pluralStringResource(R.plurals.search_result_count, state.total, state.total))
            }
            if ((state.refreshRequest is RequestState.Error || state.observationRequest is RequestState.Error) &&
                state.items.isNotEmpty()) item("saved-error") {
                FeatureNotice(stringResource(R.string.search_saved_title), stringResource(R.string.search_saved_message),
                    { onIntent(SearchIntent.Reload) }, DesignR.drawable.icon_wifi_off)
            }
            if (state.items.isEmpty()) item("status") {
                when {
                    state.refreshRequest is RequestState.Loading || state.observationRequest is RequestState.Loading ->
                        FeatureLoading(stringResource(R.string.search_loading))
                    state.refreshRequest is RequestState.Error || state.observationRequest is RequestState.Error ->
                        FeatureNotice(stringResource(R.string.search_error), stringResource(R.string.search_error_message),
                            { onIntent(SearchIntent.Reload) })
                    state.hasSynced -> FeatureEmpty(stringResource(R.string.search_empty),
                        stringResource(R.string.search_empty_message), DesignR.drawable.icon_search,
                        stringResource(R.string.search_filters), { onIntent(SearchIntent.OpenFilters) })
                    else -> FeatureEmpty(stringResource(R.string.search_initial),
                        stringResource(R.string.search_initial_message), DesignR.drawable.icon_search)
                }
            }
            items(state.items, key = { it.id }) { opportunity ->
                OpportunityListItem(opportunity, { onIntent(SearchIntent.OpenCluster(opportunity.id)) },
                    { onIntent(SearchIntent.WatchCluster(opportunity.id)) },
                    pending = opportunity.id in state.pendingClusterIds,
                    actionsEnabled = state.observationRequest is RequestState.Success)
            }
            item("paging") {
                when (state.pageRequest) {
                    is RequestState.Loading -> FeatureLoading(stringResource(R.string.search_loading_more))
                    is RequestState.Error -> FeatureNotice(stringResource(R.string.search_page_error),
                        stringResource(R.string.search_page_error_message),
                        onRetry = if (state.canAppend) ({ onIntent(SearchIntent.RetryNextPage) }) else null)
                    else -> if (state.canLoadMore) FeatureAction(stringResource(R.string.search_more),
                        { onIntent(SearchIntent.LoadNextPage) }, Modifier.fillMaxWidth(), DesignR.drawable.icon_add,
                        enabled = state.canAppend)
                }
            }
        }
    }
}

@Composable
internal fun SearchFiltersScreen(state: SearchState, onIntent: (SearchIntent) -> Unit) {
    val filters = state.filterDraft ?: state.draftQuery
    FeatureScaffold(stringResource(R.string.search_filters), onBack = { onIntent(SearchIntent.ClosePanel) },
        actions = { TextButton({ onIntent(SearchIntent.ResetFilters) }) { Text(stringResource(R.string.search_reset)) } },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)) {
            item { FeaturePageHeader(stringResource(R.string.search_filter_eyebrow),
                stringResource(R.string.search_filter_title), stringResource(R.string.search_filter_subtitle)) }
            item {
                FeatureSection(stringResource(R.string.search_sort))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OpportunitySearchSort.entries.forEach { sort ->
                        FilterChip(filters.sort == sort, { onIntent(SearchIntent.SortSelected(sort)) },
                            label = { Text(stringResource(when (sort) {
                                OpportunitySearchSort.RELEVANCE -> R.string.search_relevance
                                OpportunitySearchSort.NEWEST -> R.string.search_newest
                                OpportunitySearchSort.LARGEST -> R.string.search_largest
                            })) })
                    }
                }
            }
            if (state.optionsRequest is RequestState.Loading) item { FeatureLoading(stringResource(R.string.search_options_loading)) }
            if (state.optionsRequest is RequestState.Error) item {
                FeatureNotice(stringResource(R.string.search_options_error), stringResource(R.string.search_options_retry),
                    { onIntent(SearchIntent.RetryOptions) })
            }
            item { FeatureSection(stringResource(R.string.search_sources)) }
            if (state.sourceOptions.isEmpty()) item {
                Text(stringResource(R.string.search_no_source_options), style = MaterialTheme.typography.bodySmall)
            }
            items((state.sourceOptions.map { it.lowercase() } + filters.sources).distinct(), key = { "source-" + it }) { source ->
                FeatureBullet(source, source in filters.sources, { onIntent(SearchIntent.SourceToggled(source)) })
            }
            item { FeatureSection(stringResource(R.string.search_communities)) }
            if (state.communityOptions.isEmpty()) item {
                Text(stringResource(R.string.search_no_community_options), style = MaterialTheme.typography.bodySmall)
            }
            items((state.communityOptions.map { it.lowercase() } + filters.communities).distinct(), key = { "community-" + it }) { community ->
                FeatureBullet(community, community in filters.communities,
                    { onIntent(SearchIntent.CommunityToggled(community)) })
            }
            item {
                OutlinedTextField(state.minPostsInput, { onIntent(SearchIntent.MinimumPostsChanged(it)) },
                    Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.search_minimum_posts)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
            }
            item {
                OutlinedTextField(state.observedAfterInput, { onIntent(SearchIntent.ObservedAfterChanged(it)) },
                    Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.search_after_date)) },
                    placeholder = { Text(stringResource(R.string.search_date_hint)) }, singleLine = true)
            }
            state.validationMessage?.let { message -> item { Text(stringResource(message), color = MaterialTheme.colorScheme.error) } }
            item {
                FeatureAction(stringResource(R.string.search_apply), { onIntent(SearchIntent.ApplyFilters) },
                    Modifier.fillMaxWidth(), DesignR.drawable.icon_check)
            }
        }
    }
}

@Composable
internal fun SearchAssistantScreen(state: SearchState, onIntent: (SearchIntent) -> Unit) {
    FeatureScaffold(stringResource(R.string.search_assistant), onBack = { onIntent(SearchIntent.ClosePanel) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)) {
            item { FeatureHero(stringResource(R.string.search_assistant_eyebrow),
                stringResource(R.string.search_assistant_title), stringResource(R.string.search_assistant_subtitle),
                icon = DesignR.drawable.icon_sparkles) }
            item {
                OutlinedTextField(state.assistantInput, { onIntent(SearchIntent.AssistantChanged(it)) },
                    Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.search_describe)) }, minLines = 4)
            }
            item {
                FeatureAction(stringResource(R.string.search_prepare), { onIntent(SearchIntent.InterpretQuery) },
                    Modifier.fillMaxWidth(), DesignR.drawable.icon_sparkles,
                    pending = state.interpretationRequest is RequestState.Loading)
            }
            state.validationMessage?.let { message -> item { Text(stringResource(message), color = MaterialTheme.colorScheme.error) } }
            if (state.interpretationRequest is RequestState.Error) item {
                FeatureNotice(stringResource(R.string.search_assistant_error), stringResource(R.string.search_assistant_error_message),
                    { onIntent(SearchIntent.InterpretQuery) })
            }
            state.interpretation?.let { draft ->
                item { FeaturePanel {
                    FeatureSection(stringResource(R.string.search_review))
                    Text(draft.confirmation)
                    if (draft.fallbackUsed) Text(stringResource(R.string.search_fallback),
                        style = MaterialTheme.typography.bodySmall)
                } }
                draft.clarificationQuestion?.let { question -> item { FeatureNotice(
                    stringResource(R.string.search_clarification), question, icon = DesignR.drawable.icon_message) } }
                if (draft.assumptions.isNotEmpty()) item { FeatureSection(stringResource(R.string.search_assumptions)) }
                items(draft.assumptions, key = { "assumption-" + it }) { FeatureBullet(it) }
                if (draft.unsupported.isNotEmpty()) item { FeatureSection(stringResource(R.string.search_unsupported)) }
                items(draft.unsupported, key = { "unsupported-" + it }) { FeatureBullet(it) }
                item {
                    FeatureAction(stringResource(R.string.search_confirm), { onIntent(SearchIntent.ConfirmDraft) },
                        Modifier.fillMaxWidth(), DesignR.drawable.icon_search, enabled = !draft.needsClarification && state.interpretationRequest !is RequestState.Loading &&
                            state.interpretationRequest !is RequestState.Error)
                }
            }
        }
    }
}
