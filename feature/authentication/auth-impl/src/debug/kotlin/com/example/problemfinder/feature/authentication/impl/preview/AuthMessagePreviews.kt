package com.example.problemfinder.feature.authentication.impl.preview

import androidx.compose.runtime.Composable
import com.example.problemfinder.core.designsystem.components.FeaturePreviews
import com.example.problemfinder.core.designsystem.theme.ImmensityTheme
import com.example.problemfinder.feature.authentication.impl.AuthMessageScreen
import com.example.problemfinder.feature.authentication.impl.R

/** Each email-confirmation route supplies different copy to the same production screen. */
@FeaturePreviews
@Composable
private fun SignUpEmailSentPreview() = ImmensityTheme {
    AuthMessageScreen(messageRes = R.string.auth_signup_email_sent_message, onBackToSignIn = {})
}

@FeaturePreviews
@Composable
private fun PasswordRecoveryEmailSentPreview() = ImmensityTheme {
    AuthMessageScreen(messageRes = R.string.auth_reset_email_sent_message, onBackToSignIn = {})
}
