package com.example.problemfinder.feature.browse.impl.browse

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.problemfinder.core.model.discovery.DiscoveryCluster
import com.example.problemfinder.feature.browse.impl.R
import java.time.ZoneId

@Composable
internal fun BrowseOpportunityRow(
    cluster: DiscoveryCluster,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    val zone = ZoneId.systemDefault()
    val dateLabel = remember(cluster.date, locale, zone) {
        cluster.date?.toBrowseDateLabel(locale, zone)
    }
    val description = cluster.summary?.takeIf { it.isNotBlank() }
        ?: cluster.problemStatement?.takeIf { it.isNotBlank() }

    Column(
        modifier = modifier.fillMaxWidth()
            .semantics(mergeDescendants = true) {}
            .padding(16.dp),
    ) {
        Text(
            text = cluster.name?.takeIf { it.isNotBlank() }
                ?: stringResource(R.string.title_unnamed_opportunity),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
        )
        if (description != null) {
            Spacer(Modifier.height(6.dp))
            Text(description, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            pluralStringResource(R.plurals.browse_post_count, cluster.postCount, cluster.postCount),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = if (dateLabel != null) {
                stringResource(R.string.label_latest_activity, dateLabel)
            } else {
                stringResource(R.string.label_activity_date_unavailable)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (cluster.isWatched) {
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.label_watching),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
