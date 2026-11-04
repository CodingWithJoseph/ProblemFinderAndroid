package com.example.problemfinder.core.designsystem.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.problemfinder.core.designsystem.R
import com.example.problemfinder.core.model.workspace.WorkspaceFailure
import com.example.problemfinder.core.model.workspace.WorkspaceStatus

@Composable
fun workspaceStatusLabel(status: WorkspaceStatus): String = stringResource(when (status) {
    WorkspaceStatus.NOT_STARTED -> R.string.workspace_not_started
    WorkspaceStatus.QUEUED -> R.string.workspace_queued
    WorkspaceStatus.GENERATING -> R.string.workspace_generating
    WorkspaceStatus.READY -> R.string.workspace_ready
    WorkspaceStatus.STALE -> R.string.workspace_stale
    WorkspaceStatus.INSUFFICIENT_EVIDENCE -> R.string.workspace_insufficient
    WorkspaceStatus.FAILED -> R.string.workspace_failed
})

@Composable
fun workspaceFailureMessage(failure: WorkspaceFailure): String = stringResource(when (failure) {
    WorkspaceFailure.SESSION -> R.string.workspace_session_error
    WorkspaceFailure.NETWORK -> R.string.workspace_network_error
    WorkspaceFailure.NOT_FOUND -> R.string.workspace_not_found
    WorkspaceFailure.CONFLICT -> R.string.workspace_conflict
    WorkspaceFailure.REJECTED -> R.string.workspace_rejected
    WorkspaceFailure.SERVER -> R.string.workspace_server_error
    WorkspaceFailure.INVALID_RESPONSE -> R.string.workspace_invalid_response
    WorkspaceFailure.UNKNOWN -> R.string.workspace_unknown_error
})
