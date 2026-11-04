package com.example.problemfinder.feature.opportunities.impl.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.example.problemfinder.feature.opportunities.impl.R
import com.example.problemfinder.core.designsystem.R as DesignR


@Composable
fun OpportunityRefreshAction(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    label: String = stringResource(R.string.title_refresh_button),
) {
    val refreshing = stringResource(R.string.message_refreshing)
    IconButton(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.semantics {
            contentDescription = label
            if (loading) stateDescription = refreshing
        },
    ) {
        if (loading) CircularProgressIndicator(Modifier.size(20.dp).clearAndSetSemantics {}, strokeWidth = 2.dp)
        else Icon(painterResource(DesignR.drawable.icon_refresh), contentDescription = null, modifier = Modifier.size(22.dp))
    }
}
