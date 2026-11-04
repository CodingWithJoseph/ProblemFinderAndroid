package com.example.problemfinder.app.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.problemfinder.app.R
import com.example.problemfinder.core.mvi.RequestState

/** The startup route's existing UI, extracted so it can render without navigation or account work. */
@Composable
internal fun StartupScreen(accountRequest: RequestState<String?, Unit>, onRetry: () -> Unit) {
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
                Text(stringResource(R.string.session_restore_failed), textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onRetry) { Text(stringResource(R.string.retry_label)) }
            } else {
                CircularProgressIndicator()
            }
        }
    }
}
