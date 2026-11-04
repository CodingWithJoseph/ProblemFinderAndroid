package com.example.problemfinder.core.designsystem.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.problemfinder.core.designsystem.R
import com.example.problemfinder.core.designsystem.theme.appearance
import com.example.problemfinder.core.designsystem.theme.featurePalette
import com.example.problemfinder.core.designsystem.theme.opportunityPalette
import com.example.problemfinder.core.model.details.OpportunityEvidence
import com.example.problemfinder.core.model.opportunity.Opportunity

/** Rendering contracts only. Feature ViewModels own status selection and operation handling. */
enum class FeatureContentStatus { EMPTY, LOADING, CONTENT, ERROR }

@Preview(name = "Light", widthDp = 393, heightDp = 852, showBackground = true)
@Preview(name = "Dark", widthDp = 393, heightDp = 852, showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Large text", widthDp = 320, heightDp = 852, fontScale = 2f, showBackground = true)
@Preview(name = "Landscape", widthDp = 720, heightDp = 400, showBackground = true)
annotation class FeaturePreviews

@Composable
fun FeatureIcon(@DrawableRes icon: Int, modifier: Modifier = Modifier, description: String? = null) {
    Icon(painterResource(icon), description, modifier.size(20.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeatureScaffold(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onRefresh: (() -> Unit)? = null,
    refreshing: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {},
    refreshEnabled: Boolean = true,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(title, style = MaterialTheme.typography.titleMedium) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                ),
                navigationIcon = {
                    onBack?.let { back ->
                        IconButton(onClick = back) {
                            FeatureIcon(R.drawable.icon_arrow_back, description = stringResource(R.string.ui_back))
                        }
                    }
                },
                actions = {
                    onRefresh?.let { refresh ->
                        IconButton(onClick = refresh, enabled = refreshEnabled && !refreshing) {
                            if (refreshing) CircularProgressIndicator(
                                modifier = Modifier.size(20.dp), strokeWidth = 2.dp,
                                color = opportunityPalette().foreground,
                            ) else FeatureIcon(R.drawable.icon_refresh, description = stringResource(R.string.ui_refresh))
                        }
                    }
                    actions()
                },
            )
        },
        content = content,
    )
}

@Composable
fun FeaturePageHeader(eyebrow: String, title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(eyebrow, style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.4.sp),
            color = opportunityPalette().secondaryText)
        Text(title, style = MaterialTheme.typography.headlineLarge.copy(
            fontWeight = FontWeight.SemiBold, letterSpacing = (-1.2).sp),
            modifier = Modifier.semantics { heading() })
        Text(subtitle, style = MaterialTheme.typography.bodyMedium,
            color = opportunityPalette().secondaryText)
    }
}

@Composable
fun FeaturePanel(
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = opportunityPalette()
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = if (highlighted) featurePalette().surface else palette.surface,
        contentColor = if (highlighted) featurePalette().foreground else palette.foreground,
        border = if (highlighted) null else BorderStroke(1.dp, palette.border),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
fun FeatureHero(
    eyebrow: String,
    title: String,
    summary: String,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int = R.drawable.icon_target,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    FeaturePanel(modifier, highlighted = true) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            FeatureIcon(icon)
            Text(eyebrow, color = featurePalette().actionContainer,
                style = MaterialTheme.typography.labelMedium)
        }
        Text(title, style = MaterialTheme.typography.headlineMedium.copy(
            fontWeight = FontWeight.SemiBold, letterSpacing = (-0.8).sp),
            modifier = Modifier.semantics { heading() })
        Text(summary, style = MaterialTheme.typography.bodyMedium,
            color = featurePalette().secondaryText)
        content()
    }
}

@Composable
fun FeatureSection(title: String, subtitle: String? = null, trailing: @Composable () -> Unit = {}) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, modifier = Modifier.weight(1f).semantics { heading() },
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold))
            trailing()
        }
        subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall,
            color = opportunityPalette().secondaryText) }
    }
}

@Composable
fun FeatureAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
    enabled: Boolean = true,
    pending: Boolean = false,
    prominent: Boolean = false,
    destructive: Boolean = false,
) {
    val palette = if (prominent) featurePalette() else opportunityPalette()
    Button(
        onClick = onClick, enabled = enabled && !pending,
        modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(15.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (destructive) palette.removeContainer else palette.actionContainer,
            contentColor = if (destructive) palette.removeContent else palette.actionContent,
        ),
    ) {
        if (pending) {
            CircularProgressIndicator(Modifier.size(18.dp), color = LocalContentColor.current, strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
        } else icon?.let {
            FeatureIcon(it, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun FeatureMetrics(values: List<Pair<String, String>>) {
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        values.forEach { (value, label) ->
            Column(Modifier.widthIn(min = 80.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(value, style = MaterialTheme.typography.headlineMedium)
                Text(label, style = MaterialTheme.typography.bodySmall, color = opportunityPalette().secondaryText)
            }
        }
    }
}

@Composable
fun FeatureStatus(label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier, shape = RoundedCornerShape(8.dp),
        color = opportunityPalette().border.copy(alpha = .4f),
        contentColor = opportunityPalette().foreground,
    ) { Text(label, Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
        style = MaterialTheme.typography.labelSmall) }
}

@Composable
fun FeatureNotice(
    title: String,
    message: String,
    onRetry: (() -> Unit)? = null,
    @DrawableRes icon: Int = R.drawable.icon_warning,
) {
    FeaturePanel {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FeatureIcon(icon)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(message, style = MaterialTheme.typography.bodyMedium,
                    color = opportunityPalette().secondaryText)
            }
        }
        onRetry?.let { TextButton(onClick = it) { Text(stringResource(R.string.ui_retry)) } }
    }
}

@Composable
fun FeatureEmpty(
    title: String,
    message: String,
    @DrawableRes icon: Int,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Surface(shape = RoundedCornerShape(18.dp), color = opportunityPalette().border.copy(alpha = .4f)) {
            FeatureIcon(icon, modifier = Modifier.padding(16.dp).size(28.dp))
        }
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = opportunityPalette().secondaryText)
        if (actionLabel != null && onAction != null) FeatureAction(actionLabel, onAction)
    }
}

@Composable
fun FeatureLoading(label: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        CircularProgressIndicator(Modifier.size(26.dp), strokeWidth = 2.dp,
            color = opportunityPalette().foreground)
        Text(label, style = MaterialTheme.typography.bodyMedium,
            color = opportunityPalette().secondaryText)
    }
}

@Composable
fun FeatureEntry(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int = R.drawable.icon_document,
    status: String? = null,
) {
    Surface(
        onClick = onClick, modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        color = opportunityPalette().surface, border = BorderStroke(1.dp, opportunityPalette().border),
    ) {
        Row(Modifier.padding(18.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top) {
            FeatureIcon(icon, modifier = Modifier.padding(top = 3.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                status?.let { FeatureStatus(it) }
                Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = opportunityPalette().secondaryText)
            }
            FeatureIcon(R.drawable.icon_chevron_right, description = stringResource(R.string.ui_open))
        }
    }
}

@Composable
fun OpportunityListItem(
    opportunity: Opportunity,
    onOpen: () -> Unit,
    onWatch: () -> Unit,
    pending: Boolean = false,
    actionsEnabled: Boolean = true,
) {
    val appearance = opportunity.category.appearance(isSystemInDarkTheme())
    Surface(
        onClick = onOpen, shape = RoundedCornerShape(22.dp),
        color = opportunityPalette().surface, border = BorderStroke(1.dp, opportunityPalette().border),
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FeatureIcon(appearance.icon)
                Text(stringResource(appearance.title), style = MaterialTheme.typography.labelSmall,
                    color = opportunityPalette().secondaryText)
            }
            Text(opportunity.name, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold))
            Text(opportunity.summary, maxLines = 3, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium, color = opportunityPalette().secondaryText)
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(pluralStringResource(R.plurals.ui_posts, opportunity.postCount, opportunity.postCount),
                    modifier = Modifier.align(Alignment.CenterVertically),
                    style = MaterialTheme.typography.bodySmall)
                FeatureAction(
                    label = stringResource(if (opportunity.isWatched) R.string.ui_remove else R.string.ui_add),
                    onClick = onWatch, enabled = actionsEnabled, pending = pending,
                    icon = if (opportunity.isWatched) R.drawable.icon_remove else R.drawable.icon_add,
                    destructive = opportunity.isWatched,
                )
            }
        }
    }
}

@Composable
fun EvidenceListItem(evidence: OpportunityEvidence, onOpen: () -> Unit, highlighted: Boolean = false) {
    Surface(
        onClick = onOpen, shape = RoundedCornerShape(20.dp),
        color = if (highlighted) opportunityPalette().border.copy(alpha = .45f) else opportunityPalette().surface,
        border = BorderStroke(1.dp, opportunityPalette().border),
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FeatureIcon(R.drawable.icon_message)
                Column(Modifier.weight(1f)) {
                    evidence.source?.let { Text(it, style = MaterialTheme.typography.labelSmall) }
                    evidence.community?.let { Text(it, style = MaterialTheme.typography.bodySmall,
                        color = opportunityPalette().secondaryText) }
                }
                FeatureIcon(R.drawable.icon_chevron_right, description = stringResource(R.string.ui_read_post))
            }
            Text(evidence.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
            (evidence.problemStatement ?: evidence.body)?.let {
                Text(it, maxLines = 3, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium, color = opportunityPalette().secondaryText)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                evidence.score?.let { Text(stringResource(R.string.ui_score, it), style = MaterialTheme.typography.bodySmall) }
                evidence.commentCount?.let { Text(pluralStringResource(R.plurals.ui_comments, it, it),
                    style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

@Composable
fun FeatureBullet(text: String, checked: Boolean = false, onCheckedChange: ((Boolean) -> Unit)? = null, enabled: Boolean = true) {
    val interaction = if (onCheckedChange == null) Modifier else Modifier
        .heightIn(min = 48.dp).toggleable(checked, enabled = enabled,
            role = androidx.compose.ui.semantics.Role.Checkbox, onValueChange = onCheckedChange)
    Row(Modifier.fillMaxWidth().then(interaction), verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (onCheckedChange != null) Checkbox(checked, null, enabled = enabled)
        else FeatureIcon(if (checked) R.drawable.icon_check_circle else R.drawable.icon_circle,
            modifier = Modifier.padding(top = 3.dp))
        Text(text, Modifier.weight(1f).padding(vertical = if (onCheckedChange != null) 12.dp else 0.dp),
            style = MaterialTheme.typography.bodyMedium)
    }
}
