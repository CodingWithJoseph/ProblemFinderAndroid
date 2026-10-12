package com.example.problemfinder.feature.settings.impl

import com.example.problemfinder.core.mvi.BaseMVIViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(): BaseMVIViewModel<SettingsIntent, SettingsState, SettingsEffect>(SettingsState()) {
    override fun onIntent(intent: SettingsIntent) {}
    override fun onError(error: Throwable) {}
}
