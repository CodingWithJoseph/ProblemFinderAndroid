package com.example.problemfinder.feature.problems.impl

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun ProblemsScreen(
    isSigningOut: Boolean,
    signOutFailed: Boolean,
    onSignOut: () -> Unit
){
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (signOutFailed) {
            Text(
                "Could not sign out. Please try again.",
                color = MaterialTheme.colorScheme.error
            )
        }

        Button(
            onClick = onSignOut,
            enabled = !isSigningOut
        ) {
            Text(if (isSigningOut) "Signing out…" else "Sign out")
        }
    }
}
