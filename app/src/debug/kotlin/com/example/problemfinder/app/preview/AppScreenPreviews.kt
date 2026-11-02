package com.example.problemfinder.app.preview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.example.problemfinder.app.ApplicationState
import com.example.problemfinder.app.navigation.ImmensityScaffold
import com.example.problemfinder.app.navigation.MainTab
import com.example.problemfinder.app.navigation.StartupScreen
import com.example.problemfinder.core.designsystem.components.FeaturePreviews
import com.example.problemfinder.core.designsystem.theme.ImmensityTheme
import com.example.problemfinder.core.mvi.RequestState

@FeaturePreviews
@Composable
private fun StartupLoadingPreview() = ImmensityTheme {
    Surface { StartupScreen(RequestState.Loading, {}) }
}

@FeaturePreviews
@Composable
private fun StartupRetryPreview() = ImmensityTheme {
    Surface { StartupScreen(RequestState.Error(Unit), {}) }
}

internal class MainTabPreviewProvider : PreviewParameterProvider<MainTab> {
    override val values = MainTab.entries.asSequence()
}

/** Shows the actual shared bars for every selected tab; each feature's content has its own previews. */
@FeaturePreviews
@Composable
private fun AppShellPreview(@PreviewParameter(MainTabPreviewProvider::class) tab: MainTab) = ImmensityTheme {
    ShellExample(tab)
}

@Preview(name = "Shell · signing out", widthDp = 393, heightDp = 852)
@Composable
private fun SigningOutShellPreview() = ImmensityTheme { ShellExample(MainTab.DISCOVERY, signingOut = true) }

@Preview(name = "Shell · See all back navigation", widthDp = 393, heightDp = 852)
@Composable
private fun BrowseShellPreview() = ImmensityTheme { ShellExample(MainTab.DISCOVERY, onBack = {}) }

@Composable
private fun ShellExample(tab: MainTab, signingOut: Boolean = false, onBack: (() -> Unit)? = null) {
    ImmensityScaffold(
        state = ApplicationState(isSigningOut = signingOut),
        snackbarHost = remember { SnackbarHostState() },
        showAppChrome = true, selectedTab = tab,
        onTabSelected = {}, onSettingsClicked = {}, onSignOutClicked = {}, onNavigateBack = onBack,
        content = { padding ->
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(tab.label), style = MaterialTheme.typography.headlineMedium)
            }
        },
    )
}
