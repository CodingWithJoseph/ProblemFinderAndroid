package com.example.problemfinder.feature.settings.impl

import com.example.problemfinder.core.preferences.AppearanceMode

data class SettingsState(
    val appearance: AppearanceMode = AppearanceMode.SYSTEM,
    val isSaving: Boolean = false,
    val saveFailed: Boolean = false,
)
sealed interface SettingsIntent {
    data class AppearanceSelected(val value: AppearanceMode) : SettingsIntent
    data object SignOut : SettingsIntent
}
sealed interface SettingsEffect { data object SignOut : SettingsEffect }
