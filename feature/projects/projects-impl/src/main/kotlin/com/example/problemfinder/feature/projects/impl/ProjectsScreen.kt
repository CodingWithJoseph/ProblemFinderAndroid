package com.example.problemfinder.feature.projects.impl

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.problemfinder.core.designsystem.components.*
import com.example.problemfinder.core.designsystem.theme.opportunityPalette
import com.example.problemfinder.core.designsystem.R as DesignR

@Composable
fun ProjectsScreen(state: ProjectsState, onIntent: (ProjectsIntent) -> Unit, modifier: Modifier = Modifier) {
    FeatureScaffold(stringResource(R.string.analyze_title), modifier,
        onRefresh = { onIntent(ProjectsIntent.Reload) }, refreshing = state.isRefreshing,
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).imePadding(), contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)) {
            item { FeaturePageHeader(stringResource(R.string.analyze_eyebrow), stringResource(R.string.analyze_headline),
                stringResource(R.string.analyze_subtitle)) }
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AnalysisStage.entries.forEach { stage ->
                        FilterChip(state.stage == stage, { onIntent(ProjectsIntent.StageSelected(stage)) },
                            label = { Text(stringResource(when (stage) {
                                AnalysisStage.WATCHING -> R.string.analyze_watching
                                AnalysisStage.IN_REVIEW -> R.string.analyze_in_review
                                AnalysisStage.READY -> R.string.analyze_ready
                            })) })
                    }
                }
            }
            if (state.projects.isEmpty()) item {
                when (state.status) {
                    FeatureContentStatus.LOADING -> FeatureLoading(stringResource(R.string.analyze_loading))
                    FeatureContentStatus.ERROR -> FeatureNotice(stringResource(R.string.analyze_error),
                        state.failure?.let { workspaceFailureMessage(it) } ?: stringResource(R.string.analyze_error_message), { onIntent(ProjectsIntent.Reload) })
                    else -> FeatureEmpty(stringResource(R.string.analyze_empty), stringResource(R.string.analyze_empty_message),
                        DesignR.drawable.icon_target, stringResource(R.string.analyze_discover), { onIntent(ProjectsIntent.Discover) })
                }
            }
            items(state.projects, key = { it.id }) { project ->
                FeatureEntry(project.title, project.summary, { onIntent(ProjectsIntent.OpenProject(project.id)) },
                    icon = DesignR.drawable.icon_target, status = project.workflowStatus?.let { workspaceStatusLabel(it) } ?: project.stageLabel)
            }
            if (state.projects.isNotEmpty() && state.status == FeatureContentStatus.ERROR) item("cached-error") {
                FeatureNotice(stringResource(R.string.analyze_error), state.failure?.let { workspaceFailureMessage(it) } ?: stringResource(R.string.analyze_error_message),
                    { onIntent(ProjectsIntent.Reload) })
            }
        }
    }
}

@Composable
internal fun AnalysisWorkspaceScreen(
    state: AnalysisWorkspaceState,
    onIntent: (AnalysisIntent) -> Unit,
    panel: AnalysisPanel = AnalysisPanel.OVERVIEW,
) {
    val canChange = !state.isMutating && !state.isRefreshing && state.caseStatus?.isGenerating != true
    FeatureScaffold(
        stringResource(when (panel) {
            AnalysisPanel.OVERVIEW -> R.string.analyze_workspace
            AnalysisPanel.PROBLEM -> R.string.analyze_problem
            AnalysisPanel.AUDIENCE -> R.string.analyze_audience
            AnalysisPanel.ASSUMPTIONS -> R.string.analyze_assumptions
            AnalysisPanel.CONVERSATION -> R.string.analyze_conversation
        }),
        onBack = { onIntent(AnalysisIntent.Back) },
        onRefresh = { onIntent(AnalysisIntent.Reload) },
        refreshing = state.isRefreshing,
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).imePadding(), contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)) {
            if (state.failure != null || state.status == FeatureContentStatus.ERROR) item("error") {
                FeatureNotice(stringResource(R.string.analyze_error), state.failure?.let { workspaceFailureMessage(it) } ?: stringResource(R.string.analyze_error_message),
                    { onIntent(AnalysisIntent.Reload) })
            }
            if (state.status == FeatureContentStatus.LOADING) item("loading") {
                FeatureLoading(state.progressLabel.ifEmpty { stringResource(R.string.analyze_generating) })
            }
            if (!state.safeError.isNullOrBlank()) item("generation-error") {
                FeatureNotice(stringResource(R.string.analyze_generation_issue), state.safeError)
            }
            if (state.caseStatus == com.example.problemfinder.core.model.workspace.WorkspaceStatus.STALE) item("stale") {
                FeatureNotice(stringResource(R.string.analyze_stale), stringResource(R.string.analyze_stale_message),
                    { onIntent(AnalysisIntent.Generate) })
            }
            when (panel) {
                AnalysisPanel.OVERVIEW -> {
                    val workspace = state.workspace
                    if (workspace == null && state.status != FeatureContentStatus.LOADING) item("empty") {
                        FeatureEmpty(stringResource(R.string.analyze_start), stringResource(R.string.analyze_start_message),
                            DesignR.drawable.icon_lightbulb, stringResource(R.string.analyze_generate),
                            { onIntent(AnalysisIntent.Generate) })
                    }
                    if (workspace != null) {
                        item("hero") { FeatureHero(stringResource(R.string.analyze_case_eyebrow), workspace.title,
                            workspace.summary, icon = DesignR.drawable.icon_target) {
                            Text(if (state.version > 0) stringResource(R.string.analyze_version, state.version) else workspace.versionLabel,
                                style = MaterialTheme.typography.bodySmall)
                            workspace.clusterId?.let { clusterId -> FeatureAction(stringResource(R.string.analyze_view_opportunity),
                                { onIntent(AnalysisIntent.OpenOpportunity(clusterId)) },
                                icon = DesignR.drawable.icon_arrow_up_right, prominent = true) }
                        } }
                        if (workspace.thesis.isNotBlank()) item("thesis") { FeaturePanel {
                            FeatureSection(stringResource(R.string.analyze_thesis))
                            Text(workspace.thesis, style = MaterialTheme.typography.bodyLarge)
                            FeatureAction(stringResource(if (workspace.thesisConfirmed) R.string.analyze_thesis_confirmed else R.string.analyze_confirm_thesis),
                                { onIntent(AnalysisIntent.ConfirmThesis) }, icon = DesignR.drawable.icon_check,
                                enabled = canChange && !workspace.thesisConfirmed)
                        } }
                        items(state.claims, key = { "claim-" + it.id }) { claim -> FeaturePanel {
                            Text(stringResource(when (claim.kind) {
                                "observed" -> R.string.analyze_observed
                                "user_confirmed" -> R.string.analyze_user_confirmed
                                else -> R.string.analyze_inferred
                            }), style = MaterialTheme.typography.labelSmall)
                            Text(claim.text)
                        } }
                        item("problem-heading") { FeatureSection(stringResource(R.string.analyze_problem_set)) }
                        items(workspace.problems, key = { "problem-" + it.id }) { problem ->
                            FeatureEntry(problem.title, problem.description,
                                { onIntent(AnalysisIntent.OpenProblem(problem.id)) },
                                icon = DesignR.drawable.icon_target, status = when {
                                    problem.rejected -> stringResource(R.string.analyze_set_aside)
                                    problem.pinned -> stringResource(R.string.analyze_in_focus)
                                    else -> problemKindLabel(problem.kindLabel)
                                })
                        }
                        item("audience-heading") { FeatureSection(stringResource(R.string.analyze_audience)) }
                        items(workspace.audiences, key = { "audience-" + it.id }) { audience ->
                            FeatureEntry(audience.name, audience.description,
                                { onIntent(AnalysisIntent.OpenAudience(audience.id)) }, icon = DesignR.drawable.icon_users)
                        }
                        item("assumptions-link") { FeatureEntry(stringResource(R.string.analyze_assumptions),
                            stringResource(R.string.analyze_assumptions_subtitle),
                            { onIntent(AnalysisIntent.OpenAssumptions) }, icon = DesignR.drawable.icon_warning) }
                        item("conversation-link") { FeatureEntry(stringResource(R.string.analyze_conversation),
                            stringResource(R.string.analyze_conversation_subtitle),
                            { onIntent(AnalysisIntent.OpenConversation) }, icon = DesignR.drawable.icon_message) }
                        item("build-link") { FeatureEntry(stringResource(R.string.analyze_open_build),
                            stringResource(R.string.analyze_open_build_message),
                            { onIntent(AnalysisIntent.OpenBuild) }, icon = DesignR.drawable.icon_builds) }
                        item("regenerate") { FeatureAction(stringResource(R.string.analyze_regenerate),
                            { onIntent(AnalysisIntent.Generate) }, Modifier.fillMaxWidth(), DesignR.drawable.icon_refresh,
                            enabled = canChange) }
                    }
                }
                AnalysisPanel.PROBLEM -> {
                    val problem = state.selectedProblem
                    if (problem == null) item { AnalysisSelectionMissing { onIntent(AnalysisIntent.Reload) } }
                    else {
                        item { FeaturePageHeader(problemKindLabel(problem.kindLabel), problem.title, problem.description) }
                        item { FeatureSection(stringResource(R.string.analyze_affected)) }
                        items(problem.affectedUsers) { FeatureBullet(it) }
                        item { FeaturePanel {
                            FeatureSection(stringResource(R.string.analyze_situation))
                            Text(problem.situation, style = MaterialTheme.typography.bodyLarge)
                        } }
                        item { FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            FeatureAction(stringResource(if (problem.pinned) R.string.analyze_unpin else R.string.analyze_keep),
                                { onIntent(AnalysisIntent.PinProblem(problem.id)) }, icon = DesignR.drawable.icon_check, enabled = canChange)
                            FeatureAction(stringResource(if (problem.rejected) R.string.analyze_restore else R.string.analyze_reject),
                                { onIntent(AnalysisIntent.RejectProblem(problem.id)) }, destructive = !problem.rejected, enabled = canChange)
                        } }
                        item { FeatureSection(stringResource(R.string.analyze_supporting_evidence)) }
                        items(problem.evidence, key = { it.id }) { evidence ->
                            EvidenceListItem(evidence, {
                                state.workspace?.clusterId?.let { onIntent(AnalysisIntent.OpenPost(it, evidence.id)) }
                            })
                        }
                    }
                }
                AnalysisPanel.AUDIENCE -> {
                    val audience = state.selectedAudience
                    if (audience == null) item { AnalysisSelectionMissing { onIntent(AnalysisIntent.Reload) } }
                    else {
                        item { FeaturePageHeader(stringResource(R.string.analyze_audience_eyebrow),
                            audience.name, audience.description) }
                        item { FeatureSection(stringResource(R.string.analyze_language)) }
                        items(audience.language) { FeaturePanel { Text(it) } }
                        item { FeatureSection(stringResource(R.string.analyze_communities)) }
                        items(audience.communities) { FeatureBullet(it) }
                        item { FeatureSection(stringResource(R.string.analyze_reach)) }
                        items(audience.reachChannels) { FeatureBullet(it) }
                        item { FeatureSection(stringResource(R.string.analyze_unknowns)) }
                        items(audience.unknowns) { FeatureBullet(it) }
                    }
                }
                AnalysisPanel.ASSUMPTIONS -> {
                    item { FeaturePageHeader(stringResource(R.string.analyze_validation_eyebrow),
                        stringResource(R.string.analyze_assumptions_title), stringResource(R.string.analyze_assumptions_subtitle)) }
                    if (state.workspace?.assumptions.isNullOrEmpty()) item {
                        FeatureEmpty(stringResource(R.string.analyze_no_assumptions),
                            stringResource(R.string.analyze_no_assumptions_message), DesignR.drawable.icon_check_circle)
                    }
                    items(state.workspace?.assumptions.orEmpty(), key = { it.id }) { assumption ->
                        FeaturePanel {
                            FeatureBullet(assumption.question, assumption.resolved,
                                { onIntent(AnalysisIntent.ResolveAssumption(assumption.id, it)) }, enabled = canChange)
                            Text(assumption.whyItMatters, style = MaterialTheme.typography.bodyMedium)
                            Text(stringResource(R.string.analyze_resolution), style = MaterialTheme.typography.labelLarge)
                            Text(assumption.resolutionEvidence, color = opportunityPalette().secondaryText)
                        }
                    }
                }
                AnalysisPanel.CONVERSATION -> {
                    item { FeatureHero(stringResource(R.string.analyze_conversation_eyebrow),
                        stringResource(R.string.analyze_conversation_title), stringResource(R.string.analyze_conversation_subtitle),
                        icon = DesignR.drawable.icon_message) }
                    item { FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.conversations.forEach { conversation -> FilterChip(state.conversationId == conversation.id,
                            { onIntent(AnalysisIntent.ConversationSelected(conversation.id)) },
                            label = { Text(conversation.title, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                            enabled = !state.isSending) }
                        TextButton({ onIntent(AnalysisIntent.NewConversation) }, enabled = !state.isSending) {
                            Text(stringResource(R.string.analyze_new_conversation))
                        }
                    } }
                    if (state.isLoadingConversation) item { FeatureLoading(stringResource(R.string.analyze_loading_conversation)) }
                    state.conversationFailure?.let { failure -> item {
                        FeatureNotice(stringResource(R.string.analyze_conversation_error), workspaceFailureMessage(failure),
                            { onIntent(AnalysisIntent.RetryConversation) })
                    } }
                    if (state.workspace == null) item { Text(stringResource(R.string.analyze_conversation_requires_case)) }
                    items(state.turns, key = { it.id }) { turn ->
                        FeaturePanel(highlighted = turn.isUser) {
                            Text(stringResource(if (turn.isUser) R.string.analyze_you else R.string.analyze_assistant),
                                style = MaterialTheme.typography.labelSmall)
                            Text(turn.text)
                            if (turn.insufficientEvidence) Text(stringResource(R.string.analyze_conversation_insufficient),
                                style = MaterialTheme.typography.labelSmall)
                            turn.citations.forEach { citation ->
                                TextButton({ state.workspace?.clusterId?.let { onIntent(AnalysisIntent.OpenPost(it, citation.evidenceId)) } },
                                    enabled = state.workspace?.clusterId != null) { Text(citation.label) }
                            }
                            turn.proposal?.let { proposal ->
                                HorizontalDivider()
                                Text(proposal.title, style = MaterialTheme.typography.titleSmall)
                                Text(proposal.summary)
                                if (proposal.status == "pending") FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FeatureAction(stringResource(R.string.analyze_accept_proposal),
                                        { onIntent(AnalysisIntent.DecideProposal(proposal.id, true)) }, enabled = !state.isSending)
                                    FeatureAction(stringResource(R.string.analyze_dismiss_proposal),
                                        { onIntent(AnalysisIntent.DecideProposal(proposal.id, false)) }, enabled = !state.isSending, destructive = true)
                                } else Text(stringResource(if (proposal.status == "accepted") R.string.analyze_proposal_accepted else R.string.analyze_proposal_dismissed))
                            }
                        }
                    }
                    item {
                        OutlinedTextField(state.draftMessage, { onIntent(AnalysisIntent.MessageChanged(it)) },
                            Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.analyze_message)) }, minLines = 3,
                            enabled = state.workspace != null && !state.isSending)
                    }
                    item { FeatureAction(stringResource(R.string.analyze_send),
                        { onIntent(AnalysisIntent.SendMessage) }, Modifier.fillMaxWidth(),
                        DesignR.drawable.icon_arrow_forward, pending = state.isSending,
                        enabled = state.workspace != null && state.draftMessage.trim().length in 1..4000) }
                }
            }
        }
    }
}

@Composable
private fun AnalysisSelectionMissing(onRetry: () -> Unit) {
    FeatureEmpty(stringResource(R.string.analyze_selection_missing),
        stringResource(R.string.analyze_selection_missing_message), DesignR.drawable.icon_document,
        stringResource(DesignR.string.ui_retry), onRetry)
}

@Composable
private fun problemKindLabel(kind: String): String = when (kind) {
    "cause" -> stringResource(R.string.analyze_kind_cause)
    "core_problem" -> stringResource(R.string.analyze_kind_core)
    "symptom" -> stringResource(R.string.analyze_kind_symptom)
    "consequence" -> stringResource(R.string.analyze_kind_consequence)
    "workaround" -> stringResource(R.string.analyze_kind_workaround)
    else -> kind
}
