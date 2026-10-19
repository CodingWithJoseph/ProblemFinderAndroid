package com.example.problemfinder.feature.opportunities.impl.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.example.problemfinder.core.designsystem.theme.OpportunityPalette
import com.example.problemfinder.core.model.opportunity.Opportunity
import com.example.problemfinder.feature.opportunities.impl.R
import com.example.problemfinder.core.designsystem.R as DesignR

/**
 * Uses the observed watch value to choose add/remove; pending disables only this card's action.
 * The button supplies one accessible label/state, so its decorative icon/spinner stays unlabelled.
 */
@Composable
internal fun AddRemoveButton(
    cluster: Opportunity,
    isActionPending: Boolean,
    palette: OpportunityPalette,
    onWatchAction: () -> Unit,
) {
    val container = if (cluster.isWatched) palette.removeContainer else palette.actionContainer
    val content = if (cluster.isWatched) palette.removeContent else palette.actionContent
    val action = stringResource(if (cluster.isWatched) R.string.action_remove_opportunity else R.string.action_add_opportunity, cluster.name)
    val pending = stringResource(if (cluster.isWatched) R.string.message_removing_opportunity else R.string.message_adding_opportunity)
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
            painterResource(if (cluster.isWatched) DesignR.drawable.icon_remove else DesignR.drawable.icon_add),
            contentDescription = null, modifier = Modifier.size(20.dp),
        )
    }
}
