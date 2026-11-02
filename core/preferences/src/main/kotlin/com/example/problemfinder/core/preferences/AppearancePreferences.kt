package com.example.problemfinder.core.preferences

import android.content.Context
import android.annotation.SuppressLint
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

enum class AppearanceMode { SYSTEM, LIGHT, DARK }
interface AppearancePreferences {
    val mode: StateFlow<AppearanceMode>
    suspend fun setMode(value: AppearanceMode)
}

/** Device appearance is independent of account data; publish only after persistence succeeds. */
@Singleton
class SavedAppearancePreferences @Inject constructor(@ApplicationContext context: Context) : AppearancePreferences {
    private val preferences = context.getSharedPreferences("appearance", Context.MODE_PRIVATE)
    private val mutableMode = MutableStateFlow(
        AppearanceMode.entries.firstOrNull { it.name == preferences.getString("mode", null) } ?: AppearanceMode.SYSTEM
    )
    override val mode = mutableMode.asStateFlow()
    private val mutex = Mutex()
    @SuppressLint("UseKtx") // Native commit returns the success flag; KTX edit would discard it.
    override suspend fun setMode(value: AppearanceMode) = mutex.withLock {
        withContext(Dispatchers.IO) {
            check(preferences.edit().putString("mode", value.name).commit()) { "Appearance could not be saved" }
        }
        mutableMode.value = value
    }
}

@Module @InstallIn(SingletonComponent::class)
abstract class AppearanceModule {
    @Binds abstract fun appearance(implementation: SavedAppearancePreferences): AppearancePreferences
}
