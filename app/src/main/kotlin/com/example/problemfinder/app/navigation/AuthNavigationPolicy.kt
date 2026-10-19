package com.example.problemfinder.app.navigation

import com.example.problemfinder.core.mvi.RequestState
import com.example.problemfinder.feature.authentication.api.AuthDestinations
import com.example.problemfinder.feature.opportunities.api.OpportunitiesDestinations
import io.github.jan.supabase.auth.status.SessionStatus

/**
 * Returns a replacement root, or null to keep the current destination. SDK session status and
 * cached account identity answer different questions: a token refresh failure does not itself
 * mean the user signed out. This function decides routes without mutating the back stack.
 */
internal fun resolveRootRoute(
    sessionStatus: SessionStatus,
    accountRequest: RequestState<String?, Unit>,
    linkDestination: LinkDestination?,
    currentRoute: String?,
): String? {
    val account = accountRequest as? RequestState.Success<String?>
    val hasKnownAccount = account?.value != null
    val requiresAccount = currentRoute == STARTUP_ROUTE ||
        currentRoute in signedInRoutes || currentRoute == AuthDestinations.RESET_PASSWORD

    val destination = when {
        // Successfully imported links take precedence, including when already on a signed-in page.
        linkDestination == LinkDestination.RESET_PASSWORD -> AuthDestinations.RESET_PASSWORD
        linkDestination == LinkDestination.HOME -> OpportunitiesDestinations.DISCOVERY

        // An explicit signed-out status overrides a potentially stale cached account ID.
        sessionStatus is SessionStatus.NotAuthenticated && requiresAccount ->
            AuthDestinations.SIGN_IN

        sessionStatus is SessionStatus.Authenticated &&
            (currentRoute == STARTUP_ROUTE || currentRoute == AuthDestinations.SIGN_IN) ->
            OpportunitiesDestinations.DISCOVERY

        // Cached identity can unlock local content before online session restoration finishes.
        sessionStatus == SessionStatus.Initializing &&
            currentRoute == STARTUP_ROUTE && hasKnownAccount -> OpportunitiesDestinations.DISCOVERY

        // During refresh failure, wait for identity or offer its retry screen. Only a completed
        // read with no account warrants sending a protected route back to sign-in.
        sessionStatus is SessionStatus.RefreshFailure -> when {
            accountRequest is RequestState.Error && currentRoute in signedInRoutes -> STARTUP_ROUTE
            accountRequest is RequestState.Error -> null
            account == null -> null
            hasKnownAccount &&
                (currentRoute == STARTUP_ROUTE || currentRoute == AuthDestinations.SIGN_IN) ->
                OpportunitiesDestinations.DISCOVERY
            !hasKnownAccount && requiresAccount -> AuthDestinations.SIGN_IN
            else -> null
        }

        else -> null
    }

    return destination?.takeIf { it != currentRoute }
}
