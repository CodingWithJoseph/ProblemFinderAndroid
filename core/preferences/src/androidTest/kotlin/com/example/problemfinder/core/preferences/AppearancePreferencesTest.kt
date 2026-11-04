package com.example.problemfinder.core.preferences

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppearancePreferencesTest {
    @Test fun savedModeIsReadByANewPreferencesInstance() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = context.getSharedPreferences("appearance", Context.MODE_PRIVATE)
        file.edit().clear().commit()
        try {
            val first = SavedAppearancePreferences(context)
            assertEquals(AppearanceMode.SYSTEM, first.mode.value)
            first.setMode(AppearanceMode.DARK)
            assertEquals(AppearanceMode.DARK, SavedAppearancePreferences(context).mode.value)
            first.setMode(AppearanceMode.LIGHT)
            assertEquals(AppearanceMode.LIGHT, SavedAppearancePreferences(context).mode.value)
        } finally { file.edit().clear().commit() }
    }
}
