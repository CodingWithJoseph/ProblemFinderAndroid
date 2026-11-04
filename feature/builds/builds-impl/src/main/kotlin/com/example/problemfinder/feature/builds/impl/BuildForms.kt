package com.example.problemfinder.feature.builds.impl

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.problemfinder.core.designsystem.components.*
import com.example.problemfinder.core.model.workspace.DeliveryTarget
import com.example.problemfinder.core.designsystem.R as DesignR

@Composable
internal fun BuildGenerationForm(state: BuildWorkspaceState, onIntent: (BuildIntent) -> Unit, enabled: Boolean) {
    FeaturePanel {
        FeatureSection(stringResource(R.string.build_generation_options), stringResource(R.string.build_generation_description))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DeliveryTarget.entries.forEach { target ->
                FilterChip(target in state.deliveryTargets, { onIntent(BuildIntent.TargetSelected(target)) },
                    enabled = enabled, label = { Text(stringResource(when (target) {
                        DeliveryTarget.WEB -> R.string.build_target_web
                        DeliveryTarget.IOS -> R.string.build_target_ios
                        DeliveryTarget.ANDROID -> R.string.build_target_android
                    })) })
            }
        }
        OutlinedTextField(state.constraintsInput, { onIntent(BuildIntent.ConstraintsChanged(it)) },
            Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.build_constraints_input)) },
            supportingText = { Text(stringResource(R.string.build_one_per_line)) }, minLines = 2, enabled = enabled)
        OutlinedTextField(state.preferredStackInput, { onIntent(BuildIntent.StackChanged(it)) },
            Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.build_stack_input)) },
            supportingText = { Text(stringResource(R.string.build_one_per_line)) }, minLines = 2, enabled = enabled)
        FeatureAction(stringResource(R.string.build_generate), { onIntent(BuildIntent.Generate) }, Modifier.fillMaxWidth(),
            DesignR.drawable.icon_builds, enabled = enabled, pending = state.isMutating)
    }
}

@Composable
internal fun BuildTaskUpdateForm(state: BuildWorkspaceState, onIntent: (BuildIntent) -> Unit, enabled: Boolean) {
    FeaturePanel {
        FeatureSection(stringResource(if (state.pendingTaskStatus == BuildTaskStatus.BLOCKED)
            R.string.build_blocker_title else R.string.build_verification_title))
        if (state.pendingTaskStatus == BuildTaskStatus.BLOCKED) OutlinedTextField(
            state.taskNote, { onIntent(BuildIntent.TaskNoteChanged(it)) }, Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.build_blocker_note)) }, minLines = 2, enabled = enabled)
        else {
            Text(stringResource(R.string.build_verification_description), style = MaterialTheme.typography.bodyMedium)
            OutlinedTextField(state.verificationSummary, { onIntent(BuildIntent.VerificationSummaryChanged(it)) },
                Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.build_verification_summary)) }, minLines = 2, enabled = enabled)
            OutlinedTextField(state.verificationChecks, { onIntent(BuildIntent.VerificationChecksChanged(it)) },
                Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.build_verification_checks)) },
                supportingText = { Text(stringResource(R.string.build_checks_per_line)) }, minLines = 3, enabled = enabled)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FeatureAction(stringResource(R.string.build_apply_status), { onIntent(BuildIntent.ConfirmTaskStatus) },
                enabled = enabled, pending = state.isMutating)
            TextButton({ onIntent(BuildIntent.CancelTaskStatus) }, enabled = enabled) { Text(stringResource(R.string.build_cancel)) }
        }
    }
}

@Composable
internal fun planTitle(kind: BuildPlanKind): String = stringResource(when (kind) {
    BuildPlanKind.ARCHITECTURE -> R.string.build_architecture
    BuildPlanKind.DATA -> R.string.build_data_model
    BuildPlanKind.APIS -> R.string.build_api_contracts
    BuildPlanKind.INTEGRATIONS -> R.string.build_integrations
    BuildPlanKind.SECURITY -> R.string.build_security
    BuildPlanKind.TESTS -> R.string.build_test_plan
    BuildPlanKind.DELIVERY -> R.string.build_delivery
})
