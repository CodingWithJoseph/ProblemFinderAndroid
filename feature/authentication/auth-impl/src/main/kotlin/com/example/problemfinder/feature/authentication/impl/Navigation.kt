package com.example.problemfinder.feature.authentication.impl

import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.problemfinder.feature.authentication.api.AuthDestinations
import com.example.problemfinder.feature.authentication.impl.forgotpassword.ForgotPasswordEffect
import com.example.problemfinder.feature.authentication.impl.forgotpassword.ForgotPasswordScreen
import com.example.problemfinder.feature.authentication.impl.forgotpassword.ForgotPasswordViewModel
import com.example.problemfinder.feature.authentication.impl.resetpassword.ResetPasswordEffect
import com.example.problemfinder.feature.authentication.impl.resetpassword.ResetPasswordScreen
import com.example.problemfinder.feature.authentication.impl.resetpassword.ResetPasswordViewModel
import com.example.problemfinder.feature.authentication.impl.signin.SignInEffect
import com.example.problemfinder.feature.authentication.impl.signin.SignInScreen
import com.example.problemfinder.feature.authentication.impl.signin.SignInViewModel
import com.example.problemfinder.feature.authentication.impl.signup.SignUpEffect
import com.example.problemfinder.feature.authentication.impl.signup.SignUpScreen
import com.example.problemfinder.feature.authentication.impl.signup.SignUpViewModel

/**
 * Wires each destination's ViewModel to its screen and translates effects into app-owned navigation.
 * The app chooses back-stack behavior; this graph does not hold a NavController or parse auth links.
 */
fun NavGraphBuilder.authentication(
    onSignedIn: () -> Unit,
    onOpenSignIn: () -> Unit,
    onOpenSignUp: () -> Unit,
    onOpenForgotPassword: () -> Unit,
    onPasswordUpdated: () -> Unit,
    onSignUpEmailSent: () -> Unit,
    onResetPasswordEmailSent: () -> Unit
) {
    composable(route = AuthDestinations.SIGN_IN) {
        // Each back-stack entry owns its ViewModel. State renders the form; effects request navigation.
        val viewModel = hiltViewModel<SignInViewModel>()
        val state = viewModel.state.collectAsStateWithLifecycle().value

        LaunchedEffect(viewModel) {
            viewModel.effects.collect { effect ->
                when(effect) {
                    SignInEffect.SignedIn -> onSignedIn()
                    SignInEffect.OpenSignUp -> onOpenSignUp()
                    SignInEffect.OpenForgotPassword -> onOpenForgotPassword()
                }
            }
        }

        SignInScreen(
            state = state,
            onIntent = viewModel::onIntent
        )
    }

    composable(route = AuthDestinations.SIGN_UP) {
        val viewModel = hiltViewModel<SignUpViewModel>()
        val state = viewModel.state.collectAsStateWithLifecycle().value

        LaunchedEffect(viewModel) {
            viewModel.effects.collect { effect ->
                when(effect) {
                    SignUpEffect.OpenSignIn -> onOpenSignIn()
                    SignUpEffect.EmailSent -> onSignUpEmailSent()
                }
            }
        }

        SignUpScreen(
            state = state,
            onIntent = viewModel::onIntent
        )
    }

    composable(route = AuthDestinations.FORGOT_PASSWORD) {
        val viewModel = hiltViewModel<ForgotPasswordViewModel>()
        val state = viewModel.state.collectAsStateWithLifecycle().value

        LaunchedEffect(viewModel) {
            viewModel.effects.collect { effect ->
                when(effect) {
                    ForgotPasswordEffect.OpenSignIn -> onOpenSignIn()
                    ForgotPasswordEffect.EmailSent -> onResetPasswordEmailSent()
                }
            }
        }

        ForgotPasswordScreen(
            state = state,
            onIntent = viewModel::onIntent
        )
    }

    composable(route = AuthDestinations.RESET_PASSWORD) {
        val viewModel = hiltViewModel<ResetPasswordViewModel>()
        val state = viewModel.state.collectAsStateWithLifecycle().value

        LaunchedEffect(viewModel) {
            viewModel.effects.collect { effect ->
                when(effect) {
                    ResetPasswordEffect.PasswordUpdated -> onPasswordUpdated()
                }
            }
        }

        ResetPasswordScreen(
            state = state,
            onIntent = viewModel::onIntent
        )
    }

    composable(route = AuthDestinations.SIGN_UP_EMAIL_SENT) {
        AuthMessageScreen(
            messageRes = R.string.auth_signup_email_sent_message,
            onBackToSignIn = onOpenSignIn
        )
    }

    composable(route = AuthDestinations.RESET_EMAIL_SENT) {
        AuthMessageScreen(
            messageRes = R.string.auth_reset_email_sent_message,
            onBackToSignIn = onOpenSignIn
        )
    }
}
