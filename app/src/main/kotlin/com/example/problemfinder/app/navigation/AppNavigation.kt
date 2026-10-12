package com.example.problemfinder.app.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.problemfinder.app.ApplicationEffect
import com.example.problemfinder.app.ApplicationIntent
import com.example.problemfinder.app.ApplicationViewModel
import com.example.problemfinder.app.R
import com.example.problemfinder.core.mvi.RequestState
import com.example.problemfinder.feature.authentication.api.AuthDestinations
import com.example.problemfinder.feature.authentication.impl.authentication
import com.example.problemfinder.feature.browse.api.BrowseDestination
import com.example.problemfinder.feature.browse.impl.browse
import com.example.problemfinder.feature.builds.api.BuildsDestination
import com.example.problemfinder.feature.builds.impl.builds
import com.example.problemfinder.feature.projects.api.ProjectsDestination
import com.example.problemfinder.feature.projects.impl.projects
import com.example.problemfinder.feature.search.api.SearchDestination
import com.example.problemfinder.feature.search.impl.search
import com.example.problemfinder.feature.settings.api.SettingsDestination
import com.example.problemfinder.feature.settings.impl.settings
import io.github.jan.supabase.auth.status.SessionStatus

internal const val STARTUP_ROUTE = "startup"

internal val signedInRoutes = setOf(
    BrowseDestination.ROUTE,
    SearchDestination.ROUTE,
    ProjectsDestination.ROUTE,
    BuildsDestination.ROUTE,
    SettingsDestination.ROUTE
)

enum class LinkDestination {
    RESET_PASSWORD,
    HOME
}

fun NavGraphBuilder.startup(
    accountRequest: RequestState<String?, Unit>,
    onRetry: () -> Unit,
) {
    composable(STARTUP_ROUTE) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val viewportHeight = maxHeight
            Column(
                modifier = Modifier.fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = viewportHeight)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (accountRequest is RequestState.Error) {
                    Text(
                        stringResource(R.string.session_restore_failed),
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(16.dp))
                    TextButton(onClick = onRetry) {
                        Text(stringResource(R.string.retry_label))
                    }
                } else {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
fun AppNavigation(
    sessionStatus: SessionStatus,
    linkDestination: LinkDestination?,
    onLinkDestinationConsumed: () -> Unit
) {
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route


    val viewmodel = hiltViewModel<ApplicationViewModel>()
    val state = viewmodel.state.collectAsStateWithLifecycle().value


    val snackbarHostState = remember { SnackbarHostState() }
    val failureMessage = stringResource(R.string.sign_out_failed)
    val retryLabel = stringResource(R.string.retry_label)

    fun navigateToRoot(route: String) {
        if (navController.currentDestination?.route == route) return

        navController.navigate(route) {
            popUpTo(navController.graph.id) {
                inclusive = false
            }
            launchSingleTop = true
        }
    }

    val selectedTab = MainTab.entries.firstOrNull { it.route == currentRoute }
    val showChromeApp = currentRoute in signedInRoutes && currentRoute != SettingsDestination.ROUTE

    ImmensityScaffold(
        state = state,
        snackbarHost = snackbarHostState,
        showAppChrome = showChromeApp,
        selectedTab = selectedTab,
        onTabSelected = { tab ->
            navController.navigate(tab.route) {
                popUpTo(BrowseDestination.ROUTE)
                launchSingleTop = true
            }
        },
        onSettingsClicked = {
            navController.navigate(SettingsDestination.ROUTE) {
                launchSingleTop = true
            }
        },
        onSignOutClicked = {
            viewmodel.onIntent(ApplicationIntent.SignOutClicked)
        },
        content = { innerPadding ->
            val padding = if (showChromeApp) innerPadding else PaddingValues(0.dp)

            NavHost(
                navController = navController,
                startDestination = STARTUP_ROUTE,
                modifier = Modifier.padding(padding)
            ) {
                startup(
                    accountRequest = state.accountRequest,
                    onRetry = { viewmodel.onIntent(ApplicationIntent.RetryAccountRead) },
                )

                authentication(
                    onSignedIn = {
                        navigateToRoot(BrowseDestination.ROUTE)
                    },

                    onOpenSignIn = {
                        navigateToRoot(AuthDestinations.SIGN_IN)
                    },

                    onOpenSignUp = {
                        navController.navigate(AuthDestinations.SIGN_UP) {
                            launchSingleTop = true
                        }
                    },

                    onOpenForgotPassword = {
                        navController.navigate(AuthDestinations.FORGOT_PASSWORD) {
                            launchSingleTop = true
                        }
                    },

                    onPasswordUpdated = {
                        navigateToRoot(BrowseDestination.ROUTE)
                    },

                    onSignUpEmailSent = {
                        navigateToRoot(AuthDestinations.SIGN_UP_EMAIL_SENT)
                    },

                    onResetPasswordEmailSent = {
                        navigateToRoot(AuthDestinations.RESET_EMAIL_SENT)
                    }
                )

                settings(
                    onClose = {
                        navController.popBackStack()
                    }
                )

                browse()
                search()
                projects()
                builds()

            }
        }
    )

    LaunchedEffect(sessionStatus, state.accountRequest, linkDestination, currentRoute) {
        val nextRoute = resolveRootRoute(
            sessionStatus = sessionStatus,
            accountRequest = state.accountRequest,
            linkDestination = linkDestination,
            currentRoute = currentRoute,
        )

        if (nextRoute != null) navigateToRoot(nextRoute)

        if (linkDestination != null) onLinkDestinationConsumed()
    }

    LaunchedEffect(viewmodel) {
        viewmodel.effects.collect {
            when(it) {
                ApplicationEffect.SignOutFailed -> {
                    val result = snackbarHostState.showSnackbar(
                        message = failureMessage,
                        actionLabel = retryLabel
                    )

                    if (result == SnackbarResult.ActionPerformed){
                        viewmodel.onIntent(ApplicationIntent.SignOutClicked)
                    }
                }
            }
        }
    }
}

