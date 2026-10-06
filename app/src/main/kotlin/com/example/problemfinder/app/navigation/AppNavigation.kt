package com.example.problemfinder.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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

private const val STARTUP_ROUTE = "startup"

private val signedInRoutes = setOf(
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

fun NavGraphBuilder.startup() {
    composable(STARTUP_ROUTE) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
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
        onTabSelected = {
            navController.navigate(it.route) {
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
                startup()
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

    LaunchedEffect(sessionStatus, linkDestination, currentRoute, viewmodel) {

        val nextRoute = when {
            linkDestination == LinkDestination.RESET_PASSWORD -> AuthDestinations.RESET_PASSWORD
            linkDestination == LinkDestination.HOME -> BrowseDestination.ROUTE

            currentRoute == STARTUP_ROUTE -> when (sessionStatus) {
                is SessionStatus.Authenticated -> BrowseDestination.ROUTE
                is SessionStatus.NotAuthenticated -> AuthDestinations.SIGN_IN
                is SessionStatus.RefreshFailure -> AuthDestinations.SIGN_IN
                SessionStatus.Initializing -> null
            }

            (sessionStatus is SessionStatus.NotAuthenticated || sessionStatus is SessionStatus.RefreshFailure) &&
                (
                    currentRoute in signedInRoutes || currentRoute == AuthDestinations.RESET_PASSWORD
                    ) -> AuthDestinations.SIGN_IN

            else -> null
        }

        if (nextRoute != null && nextRoute != currentRoute) {
            navigateToRoot(nextRoute)
        }

        if (linkDestination != null) {
            onLinkDestinationConsumed()
        }
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

