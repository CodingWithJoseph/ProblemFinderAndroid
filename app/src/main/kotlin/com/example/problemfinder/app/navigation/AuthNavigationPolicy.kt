package com.example.problemfinder.app.navigation

import com.example.problemfinder.core.mvi.RequestState
import com.example.problemfinder.feature.authentication.api.AuthDestinations
import com.example.problemfinder.feature.opportunities.api.OpportunitiesDestinations
import io.github.jan.supabase.auth.status.SessionStatus

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
        linkDestination == LinkDestination.RESET_PASSWORD -> AuthDestinations.RESET_PASSWORD
        linkDestination == LinkDestination.HOME -> OpportunitiesDestinations.DISCOVERY

        sessionStatus is SessionStatus.NotAuthenticated && requiresAccount ->
            AuthDestinations.SIGN_IN

        sessionStatus is SessionStatus.Authenticated &&
            (currentRoute == STARTUP_ROUTE || currentRoute == AuthDestinations.SIGN_IN) ->
            OpportunitiesDestinations.DISCOVERY

        sessionStatus == SessionStatus.Initializing &&
            currentRoute == STARTUP_ROUTE && hasKnownAccount -> OpportunitiesDestinations.DISCOVERY

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
