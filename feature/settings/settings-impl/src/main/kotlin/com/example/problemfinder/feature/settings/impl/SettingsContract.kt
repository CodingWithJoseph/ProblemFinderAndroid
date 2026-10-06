package com.example.problemfinder.feature.settings.impl

data class SettingsState(
    val id: String = "Settings"
)

sealed interface SettingsIntent
sealed interface SettingsEffect
