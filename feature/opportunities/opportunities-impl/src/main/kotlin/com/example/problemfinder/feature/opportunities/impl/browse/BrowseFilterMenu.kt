package com.example.problemfinder.feature.opportunities.impl.browse

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp
import com.example.problemfinder.core.common.shared.OpportunityCategoryFilter
import com.example.problemfinder.core.common.shared.SortType
import com.example.problemfinder.core.designsystem.theme.appearance
import com.example.problemfinder.core.model.opportunity.OpportunityCategory
import com.example.problemfinder.feature.opportunities.impl.R
import com.example.problemfinder.core.designsystem.R as DesignR

/** Non-ALL filter names must match OpportunityCategory names to reuse their presentation metadata. */
@StringRes internal fun OpportunityCategoryFilter.titleRes(): Int =
    if (this == OpportunityCategoryFilter.ALL) R.string.title_browse_all
    else OpportunityCategory.valueOf(name).appearance().title

@StringRes internal fun SortType.titleRes(): Int = when (this) {
    SortType.NEWEST -> R.string.label_sort_newest
    SortType.OLDEST -> R.string.label_sort_oldest
    SortType.LARGEST -> R.string.label_sort_largest
    SortType.SMALLEST -> R.string.label_sort_smallest
}

/** Owns only menu visibility; selected filters remain in ViewModel state and change through intents. */
@Composable
internal fun BrowseFilterMenu(state: BrowseState, onIntent: (BrowseIntent) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Row {
        IconButton(onClick = { expanded = !expanded }) {
            Icon(painterResource(DesignR.drawable.icon_filter), stringResource(R.string.action_filter_sort), Modifier.size(22.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Text(stringResource(R.string.label_filter_category), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            OpportunityCategoryFilter.entries.forEach { category ->
                FilterChoice(stringResource(category.titleRes()), state.category == category) {
                    expanded = false
                    onIntent(BrowseIntent.ChangeCategory(category))
                }
            }
            HorizontalDivider()
            Text(stringResource(R.string.label_sort_by), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            SortType.entries.forEach { sort ->
                FilterChoice(stringResource(sort.titleRes()), state.sort == sort) {
                    expanded = false
                    onIntent(BrowseIntent.ChangeSortType(sort))
                }
            }
        }
    }
}

@Composable
private fun FilterChoice(label: String, chosen: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label) }, onClick = onClick,
        modifier = Modifier.semantics { selected = chosen },
        leadingIcon = {
            if (chosen) Icon(painterResource(DesignR.drawable.icon_check), contentDescription = null, modifier = Modifier.size(20.dp))
            else Spacer(Modifier.size(20.dp))
        },
    )
}
