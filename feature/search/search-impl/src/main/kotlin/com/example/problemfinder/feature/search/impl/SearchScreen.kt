package com.example.problemfinder.feature.search.impl

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.problemfinder.core.designsystem.theme.ImmensityTheme

/** Editing and intent callbacks are wired. Complete result states and visual design at SEARCH-6. */
@Composable
fun SearchScreen(
    state: SearchState,
    onIntent: (SearchIntent) -> Unit,
    modifier: Modifier = Modifier,
) = Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "search-input") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.title_search), style = MaterialTheme.typography.headlineSmall)
                OutlinedTextField(
                    value = state.draftQuery.query,
                    onValueChange = { onIntent(SearchIntent.QueryChanged(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.label_search_query)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        if (!state.implementationPending) onIntent(SearchIntent.Submit)
                    }),
                )
                Button(enabled = !state.implementationPending, onClick = { onIntent(SearchIntent.Submit) }) {
                    Text(stringResource(R.string.action_search))
                }
                if (state.implementationPending) Text(stringResource(R.string.message_search_scaffold))
                // TODO [SEARCH-6] Add source/community/min-post/date/sort controls that edit draftQuery.
                // /clusters/search/options supplies valid source/community choices; add its DTO,
                // remote call and use case before exposing dynamic options. Industry/category needs
                // a backend change. Keep filters in a menu/sheet and show applied filters distinctly.
            }
        }
        // TODO [SEARCH-6] Render initial/validation/loading/empty/content/error states separately.
        // Zero results is valid only after a successful sync. Cached content stays during refresh.
        // Add icon refresh, paging footer/retry and shared watch controls with per-ID pending state.
        // Use result cards shared through a suitable common UI module, not a sibling impl dependency.
        items(state.items, key = { it.id }) { opportunity ->
            TextButton(onClick = { onIntent(SearchIntent.OpenCluster(opportunity.id)) }) {
                Text(opportunity.name)
            }
        }
        // TODO [SEARCH-8] Add a separate assistant action and a confirmation/clarification section.
        // Render interpretation.confirmation, assumptions, unsupported and fallback status. Only
        // ConfirmDraft executes results; a clarification must remain an editable draft. Keep the
        // current manual results visible while interpretation runs or fails.
    }
}

@Preview(name = "Search scaffold", widthDp = 393, heightDp = 850)
@Preview(name = "Search scaffold dark", widthDp = 393, heightDp = 850, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Search scaffold large text", widthDp = 320, heightDp = 640, fontScale = 2f)
@Composable
private fun SearchPreview() = ImmensityTheme {
    SearchScreen(SearchState(), {})
    // TODO [SEARCH-6] Add preview-only fixtures for initial, invalid input, results, empty success,
    // cached refresh failure, page retry, and assistant clarification. Do not seed runtime fake data.
}
