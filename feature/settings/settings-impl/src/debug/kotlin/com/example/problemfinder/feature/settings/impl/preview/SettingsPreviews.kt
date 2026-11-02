package com.example.problemfinder.feature.settings.impl.preview

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.problemfinder.core.designsystem.components.FeaturePreviews
import com.example.problemfinder.core.designsystem.theme.ImmensityTheme
import com.example.problemfinder.core.preferences.AppearanceMode
import com.example.problemfinder.feature.settings.impl.*

@Preview(name = "Saved dark setting on a light device", widthDp = 393, heightDp = 852)
@Composable
private fun SavedDarkAppearancePreview() = ImmensityTheme(darkTheme = true) {
    SettingsScreen({}, state = SettingsState(appearance = AppearanceMode.DARK), appVersion = "0.1.0")
}
@Preview(name = "Saved light setting on a dark device", widthDp = 393, heightDp = 852,
    uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SavedLightAppearancePreview() = ImmensityTheme(darkTheme = false) {
    SettingsScreen({}, state = SettingsState(appearance = AppearanceMode.LIGHT), appVersion = "0.1.0")
}
@FeaturePreviews
@Composable
private fun SettingsSaveFailurePreview() = ImmensityTheme {
    SettingsScreen({}, state = SettingsState(saveFailed = true), appVersion = "0.1.0")
}
