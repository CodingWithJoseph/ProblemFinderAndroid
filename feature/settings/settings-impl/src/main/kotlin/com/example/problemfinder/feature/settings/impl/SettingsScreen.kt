package com.example.problemfinder.feature.settings.impl

import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.problemfinder.core.designsystem.components.*
import com.example.problemfinder.core.designsystem.theme.ImmensityTheme
import com.example.problemfinder.core.preferences.AppearanceMode
import com.example.problemfinder.core.designsystem.R as DesignR

@FeaturePreviews
@Composable
private fun SettingsScreenPreview() = ImmensityTheme {
    SettingsScreen(onClose = {}, appVersion = "0.1.0")
}

@Composable
fun SettingsScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    state: SettingsState = SettingsState(),
    onIntent: (SettingsIntent) -> Unit = {},
    appVersion: String = "",
    isSigningOut: Boolean = false,
) = FeatureScaffold(stringResource(R.string.feature_name), modifier, onBack = onClose) { padding ->
    LazyColumn(Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        item { FeaturePageHeader(stringResource(R.string.settings_eyebrow),
            stringResource(R.string.settings_title), stringResource(R.string.settings_description)) }
        item {
            FeaturePanel {
                FeatureSection(stringResource(R.string.settings_appearance), stringResource(R.string.settings_appearance_description))
                AppearanceMode.entries.forEach { mode ->
                    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp)
                        .selectable(selected = state.appearance == mode, enabled = !state.isSaving, role = Role.RadioButton) { onIntent(SettingsIntent.AppearanceSelected(mode)) }
                        .padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = state.appearance == mode, onClick = null, enabled = !state.isSaving)
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(when (mode) {
                            AppearanceMode.SYSTEM -> R.string.settings_system
                            AppearanceMode.LIGHT -> R.string.settings_light
                            AppearanceMode.DARK -> R.string.settings_dark
                        }))
                    }
                }
                if (state.saveFailed) Text(stringResource(R.string.settings_save_failed), color = MaterialTheme.colorScheme.error)
            }
        }
        if (appVersion.isNotEmpty()) item { FeaturePanel {
            FeatureSection(stringResource(R.string.settings_about))
            Text(stringResource(R.string.settings_version, appVersion), style = MaterialTheme.typography.bodyMedium)
        } }
        item { FeatureAction(stringResource(R.string.settings_sign_out), { onIntent(SettingsIntent.SignOut) },
            Modifier.fillMaxWidth(), DesignR.drawable.icon_sign_out, destructive = true, pending = isSigningOut,
            enabled = !state.isSaving && !isSigningOut) }
    }
}
