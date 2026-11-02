package com.example.problemfinder.app

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.jan.supabase.auth.auth
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.problemfinder.app.navigation.AppNavigation
import com.example.problemfinder.app.navigation.LinkDestination
import com.example.problemfinder.core.designsystem.theme.ImmensityTheme
import com.example.problemfinder.core.preferences.AppearanceMode
import com.example.problemfinder.core.preferences.AppearancePreferences
import dagger.hilt.android.AndroidEntryPoint
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.handleDeeplinks
import javax.inject.Inject

/** Android entry point: installs the theme and hands session/link state to app navigation. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var supabaseClient: SupabaseClient
    @Inject lateinit var appearance: AppearancePreferences
    private var pendingLinkDestination by mutableStateOf<LinkDestination?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleAuthLink(intent)

        enableEdgeToEdge()
        setContent {
            val sessionStatus by supabaseClient.auth.sessionStatus.collectAsStateWithLifecycle()
            val mode by appearance.mode.collectAsStateWithLifecycle()
            val darkTheme = when (mode) {
                AppearanceMode.SYSTEM -> isSystemInDarkTheme()
                AppearanceMode.LIGHT -> false
                AppearanceMode.DARK -> true
            }
            SideEffect {
                val bars = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) { darkTheme }
                enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
            }

            ImmensityTheme(darkTheme = darkTheme) {
                AppNavigation(
                    sessionStatus = sessionStatus,
                    linkDestination = pendingLinkDestination,
                    onLinkDestinationConsumed = {
                        pendingLinkDestination = null
                    }
                )
            }
        }
    }

    // A link can arrive while this singleTop activity is already running, not only at cold start.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAuthLink(intent)
    }

    /**
     * Accepts only the app's auth routes. The destination becomes pending only after Supabase
     * successfully imports the link's session; AppNavigation consumes it after choosing a root.
     */
    private fun handleAuthLink(intent: Intent){
        val uri = intent.data ?: return

        if (uri.scheme != "com.codingwithjoseph.immensity" || uri.host != "auth") return

        val destination = when (uri.path) {
            "/reset-password" -> LinkDestination.RESET_PASSWORD
            null, "", "/" -> LinkDestination.HOME
            else -> return
        }

        supabaseClient.handleDeeplinks(
            intent,
            onSessionSuccess = {
                runOnUiThread {
                    pendingLinkDestination = destination
                }
            },
            onError = {
                runOnUiThread {
                    Toast.makeText(
                        this,
                        getString(R.string.auth_link_open_failed),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        )
    }
}

