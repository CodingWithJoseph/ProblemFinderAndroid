package com.example.problemfinder.feature.problemfinder.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Stateless UI seam. Replace this placeholder with the product UI. */
@Composable
fun ProblemFinderScreen(
    state: ProblemFinderState,
    onIntent: (ProblemFinderIntent) -> Unit,
) {
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = "Problem Finder")
            Text(text = "Compose screen scaffold — implement the UI and intent dispatch here.")
        }
    }

    // TODO: Render state and dispatch user actions through onIntent.
    @Suppress("UNUSED_VARIABLE")
    val reservedState = state
    @Suppress("UNUSED_VARIABLE")
    val reservedIntentDispatcher = onIntent
}

