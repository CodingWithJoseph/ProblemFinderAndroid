package com.example.problemfinder.feature.settings.impl

import androidx.lifecycle.ViewModelStore
import com.example.problemfinder.core.preferences.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private class Preferences : AppearancePreferences {
        override val mode = MutableStateFlow(AppearanceMode.DARK)
        var save: suspend (AppearanceMode) -> Unit = { mode.value = it }
        var calls = 0
        override suspend fun setMode(value: AppearanceMode) { calls++; save(value) }
    }
    private val preferences = Preferences()
    @Before fun before() = Dispatchers.setMain(dispatcher)
    @After fun after() { store.clear(); dispatcher.scheduler.runCurrent(); Dispatchers.resetMain() }
    private fun model() = SettingsViewModel(preferences).also { store.put("settings", it) }

    @Test fun initialStateUsesPersistedAppearance() {
        assertEquals(AppearanceMode.DARK, model().state.value.appearance)
    }
    @Test fun saveFailureLeavesPersistedAppearanceInPlaceAndRetryWorks() = runTest {
        preferences.save = { throw IllegalStateException("Disk failure") }
        val model = model()
        model.onIntent(SettingsIntent.AppearanceSelected(AppearanceMode.LIGHT)); runCurrent()
        assertTrue(model.state.value.saveFailed)
        assertFalse(model.state.value.isSaving)
        assertEquals(AppearanceMode.DARK, model.state.value.appearance)
        preferences.save = { preferences.mode.value = it }
        model.onIntent(SettingsIntent.AppearanceSelected(AppearanceMode.LIGHT)); runCurrent()
        assertFalse(model.state.value.saveFailed)
        assertEquals(AppearanceMode.LIGHT, model.state.value.appearance)
    }
    @Test fun duplicateTapsDoNotStartCompetingPreferenceWrites() = runTest {
        val complete = CompletableDeferred<Unit>()
        preferences.save = { complete.await(); preferences.mode.value = it }
        val model = model()
        model.onIntent(SettingsIntent.AppearanceSelected(AppearanceMode.LIGHT))
        model.onIntent(SettingsIntent.AppearanceSelected(AppearanceMode.SYSTEM)); runCurrent()
        assertEquals(1, preferences.calls)
        complete.complete(Unit); runCurrent()
        assertEquals(AppearanceMode.LIGHT, model.state.value.appearance)
        assertFalse(model.state.value.isSaving)
    }
}
