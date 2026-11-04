package com.example.problemfinder.feature.settings.impl

import com.example.problemfinder.core.mvi.BaseMVIViewModel
import com.example.problemfinder.core.preferences.AppearancePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(private val appearance: AppearancePreferences) :
    BaseMVIViewModel<SettingsIntent, SettingsState, SettingsEffect>(SettingsState(appearance = appearance.mode.value)) {
    init { safeLaunch { appearance.mode.collect { reduce { copy(appearance = it) } } } }
    override fun onIntent(intent: SettingsIntent) {
        when (intent) {
            SettingsIntent.SignOut -> sendEffect(SettingsEffect.SignOut)
            is SettingsIntent.AppearanceSelected -> {
                if (state.value.isSaving || state.value.appearance == intent.value) return
                reduce { copy(isSaving = true, saveFailed = false) }
                safeLaunch {
                    try { appearance.setMode(intent.value) }
                    catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { reduce { copy(saveFailed = true) } }
                    finally { reduce { copy(isSaving = false) } }
                }
            }
        }
    }
    override fun onError(error: Throwable) { reduce { copy(isSaving = false, saveFailed = true) } }
}
