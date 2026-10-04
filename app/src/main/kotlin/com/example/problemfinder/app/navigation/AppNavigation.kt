package com.example.problemfinder.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.problemfinder.app.ApplicationViewModel
import com.example.problemfinder.feature.authentication.api.AuthDestinations
import com.example.problemfinder.feature.authentication.impl.authentication
import com.example.problemfinder.feature.problems.api.ProblemsDestination
import com.example.problemfinder.feature.problems.impl.problemsScreen
import io.github.jan.supabase.auth.status.SessionStatus

private const val STARTUP_ROUTE = "startup"

enum class LinkDestination {
    RESET_PASSWORD,
    HOME
}

@Composable
fun AppNavigation(
    sessionStatus: SessionStatus,
    linkDestination: LinkDestination?,
    onLinkDestinationConsumed: () -> Unit
) {
    val navController = rememberNavController()
    val viewmodel = hiltViewModel<ApplicationViewModel>()

    fun navigateToRoot(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.id) {
                inclusive = false
            }
            launchSingleTop = true
        }
    }

    NavHost(navController = navController, startDestination = STARTUP_ROUTE) {
        composable(STARTUP_ROUTE) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        authentication(
            onSignedIn = {
                navigateToRoot(ProblemsDestination.PROBLEMS)
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
                navigateToRoot(ProblemsDestination.PROBLEMS)
            },

            onSignUpEmailSent = {
                navigateToRoot(AuthDestinations.SIGN_UP_EMAIL_SENT)
            },

            onResetPasswordEmailSent = {
                navigateToRoot(AuthDestinations.RESET_EMAIL_SENT)
            }
        )
        problemsScreen(
            isSigningOut = { viewmodel.isSigningOut },
            signOutFailed = { viewmodel.signOutFailed },
            onSignOut = viewmodel::signOut
        )
    }

    LaunchedEffect(sessionStatus, linkDestination) {
        val route = navController.currentDestination?.route

        val nextRoute = when {
            linkDestination == LinkDestination.RESET_PASSWORD -> AuthDestinations.RESET_PASSWORD
            linkDestination == LinkDestination.HOME -> ProblemsDestination.PROBLEMS

            route == STARTUP_ROUTE -> when (sessionStatus) {
                is SessionStatus.Authenticated -> ProblemsDestination.PROBLEMS
                is SessionStatus.NotAuthenticated -> AuthDestinations.SIGN_IN
                is SessionStatus.RefreshFailure -> AuthDestinations.SIGN_IN
                SessionStatus.Initializing -> null
            }

            sessionStatus is SessionStatus.NotAuthenticated &&
                (
                    route == ProblemsDestination.PROBLEMS || route == AuthDestinations.RESET_PASSWORD
                    ) -> AuthDestinations.SIGN_IN

            else -> null
        }

        if(nextRoute != null && nextRoute != route) {
            navigateToRoot(nextRoute)
        }

        if (linkDestination != null){
            onLinkDestinationConsumed()
        }
    }
}
