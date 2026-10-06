package com.example.problemfinder.feature.settings.impl

import com.example.problemfinder.core.mvi.BaseMVIViewModel
import javax.inject.Inject

class SettingsViewModel @Inject constructor(): BaseMVIViewModel<SettingsIntent, SettingsState, SettingsEffect>(SettingsState()) {
    override fun onIntent(intent: SettingsIntent) {
        TODO("Not yet implemented")
    }
}
