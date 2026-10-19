package com.example.problemfinder.app.navigation

import com.example.problemfinder.core.mvi.RequestState
import com.example.problemfinder.feature.authentication.api.AuthDestinations
import com.example.problemfinder.feature.opportunities.api.OpportunitiesDestinations
import io.github.jan.supabase.auth.status.RefreshFailureCause
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserSession
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthNavigationPolicyTest {
    private val knownAccount: RequestState<String?, Unit> = RequestState.Success("user-a")
    private val missingAccount: RequestState<String?, Unit> = RequestState.Success(null)
    private val refreshFailure = SessionStatus.RefreshFailure(RefreshFailureCause.NetworkError(IOException("Offline")))
    private val authenticated = SessionStatus.Authenticated(
        UserSession("fixture-access-token", "fixture-refresh-token", expiresIn = 3_600, tokenType = "bearer"),
    )

    @Test
    fun offlineKnownAccountCanOpenBrowseFromStartup() {
        assertEquals(OpportunitiesDestinations.DISCOVERY, route(refreshFailure, knownAccount, STARTUP_ROUTE))
    }

    @Test
    fun refreshFailureDoesNotEvictKnownAccountFromSignedInRoutes() {
        for (current in signedInRoutes + AuthDestinations.RESET_PASSWORD) {
            assertNull(route(refreshFailure, knownAccount, current))
        }
    }

    @Test
    fun completedReadWithoutAnAccountRequiresSignIn() {
        for (current in signedInRoutes + STARTUP_ROUTE + AuthDestinations.RESET_PASSWORD) {
            assertEquals(AuthDestinations.SIGN_IN, route(refreshFailure, missingAccount, current))
        }
        assertNull(route(refreshFailure, missingAccount, AuthDestinations.SIGN_IN))
    }

    @Test
    fun refreshFailureWaitsForIdentityRatherThanAssumingSignOut() {
        for (request in listOf(RequestState.Loading, RequestState.Idle)) {
            assertNull(route(refreshFailure, request, STARTUP_ROUTE))
            assertNull(route(refreshFailure, request, OpportunitiesDestinations.DISCOVERY))
        }
    }

    @Test
    fun unreadableIdentityUsesRecoverableStartupInsteadOfSignIn() {
        assertEquals(STARTUP_ROUTE, route(refreshFailure, RequestState.Error(Unit), OpportunitiesDestinations.DISCOVERY))
        assertNull(route(refreshFailure, RequestState.Error(Unit), STARTUP_ROUTE))
        assertNull(route(refreshFailure, RequestState.Error(Unit), AuthDestinations.SIGN_IN))
    }

    @Test
    fun genuineSignOutOverridesStalePersistedIdentity() {
        for (current in signedInRoutes + STARTUP_ROUTE + AuthDestinations.RESET_PASSWORD) {
            assertEquals(AuthDestinations.SIGN_IN, route(SessionStatus.NotAuthenticated(true), knownAccount, current))
        }
    }

    @Test
    fun authenticatedSessionOpensBrowseAndRecoversASignInRoute() {
        assertEquals(OpportunitiesDestinations.DISCOVERY, route(authenticated, knownAccount, STARTUP_ROUTE))
        assertEquals(OpportunitiesDestinations.DISCOVERY, route(authenticated, RequestState.Loading, AuthDestinations.SIGN_IN))
    }

    @Test
    fun initializationCanOpenPersistedCacheBeforeNetworkRefreshCompletes() {
        assertEquals(OpportunitiesDestinations.DISCOVERY, route(SessionStatus.Initializing, knownAccount, STARTUP_ROUTE))
    }

    @Test
    fun initializationWaitsWhenNoIdentityIsKnown() {
        assertNull(route(SessionStatus.Initializing, RequestState.Loading, STARTUP_ROUTE))
        assertNull(route(SessionStatus.Initializing, missingAccount, STARTUP_ROUTE))
    }

    @Test
    fun validAuthLinksKeepTheirDestinations() {
        assertEquals(AuthDestinations.RESET_PASSWORD, resolveRootRoute(authenticated, knownAccount, LinkDestination.RESET_PASSWORD, OpportunitiesDestinations.DISCOVERY))
        assertEquals(OpportunitiesDestinations.DISCOVERY, resolveRootRoute(authenticated, knownAccount, LinkDestination.HOME, AuthDestinations.RESET_PASSWORD))
    }

    @Test
    fun authenticatedRecoveryFormAndSignedOutAuthFormsRemainInPlace() {
        assertNull(route(authenticated, knownAccount, AuthDestinations.RESET_PASSWORD))
        assertNull(route(SessionStatus.NotAuthenticated(), missingAccount, AuthDestinations.SIGN_UP))
        assertNull(route(SessionStatus.NotAuthenticated(), missingAccount, AuthDestinations.FORGOT_PASSWORD))
    }

    @Test
    fun sameDestinationDoesNotRecreateRoot() {
        assertNull(resolveRootRoute(authenticated, knownAccount, LinkDestination.HOME, OpportunitiesDestinations.DISCOVERY))
        assertNull(resolveRootRoute(authenticated, knownAccount, LinkDestination.RESET_PASSWORD, AuthDestinations.RESET_PASSWORD))
    }

    @Test
    fun noBackStackEntryWaitsForNavHostStartup() {
        assertNull(route(authenticated, knownAccount, null))
    }

    private fun route(status: SessionStatus, request: RequestState<String?, Unit>, current: String?) =
        resolveRootRoute(status, request, null, current)
}
