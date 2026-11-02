package com.example.problemfinder.feature.opportunitydetails.impl.post

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.problemfinder.core.designsystem.components.*
import com.example.problemfinder.feature.opportunitydetails.impl.R
import com.example.problemfinder.core.designsystem.R as DesignR

@Composable
internal fun PostDetailsScreen(state: PostDetailsState, onIntent: (PostDetailsIntent) -> Unit) {
    FeatureScaffold(stringResource(R.string.post_title), onBack = { onIntent(PostDetailsIntent.Back) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            val post = state.post
            if (post == null) item("status") {
                when (state.status) {
                    FeatureContentStatus.LOADING -> FeatureLoading(stringResource(R.string.post_loading))
                    FeatureContentStatus.ERROR -> FeatureNotice(stringResource(R.string.post_error),
                        stringResource(R.string.post_error_message), { onIntent(PostDetailsIntent.Retry) })
                    else -> FeatureEmpty(
                        stringResource(R.string.post_missing), stringResource(R.string.post_missing_message),
                        DesignR.drawable.icon_document, stringResource(DesignR.string.ui_retry),
                        { onIntent(PostDetailsIntent.Retry) },
                    )
                }
            } else {
                if (state.status == FeatureContentStatus.ERROR) item("cached-error") {
                    FeatureNotice(stringResource(R.string.post_error), stringResource(R.string.post_error_message),
                        { onIntent(PostDetailsIntent.Retry) })
                }
                item("header") {
                    FeaturePageHeader(post.source ?: stringResource(R.string.post_source),
                        post.title, post.community ?: stringResource(R.string.post_community_unknown))
                }
                item("metadata") {
                    FeaturePanel {
                        post.author?.let { Text(stringResource(R.string.post_author, it)) }
                        post.postedAt?.let { Text(it.toString().substringBefore('T')) }
                        post.score?.let { Text(stringResource(DesignR.string.ui_score, it)) }
                        post.commentCount?.let { Text(pluralStringResource(DesignR.plurals.ui_comments, it, it)) }
                    }
                }
                post.problemStatement?.let { problem -> item("problem") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        FeatureSection(stringResource(R.string.post_problem))
                        Text(problem, style = MaterialTheme.typography.bodyLarge)
                    }
                } }
                item("body") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        FeatureSection(stringResource(R.string.post_content))
                        SelectionContainer {
                            Text(post.body ?: stringResource(R.string.post_body_missing),
                                style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
                post.url?.let { url -> item("original") {
                    FeatureAction(stringResource(R.string.post_open_original),
                        { onIntent(PostDetailsIntent.OpenOriginal(url)) },
                        Modifier.fillMaxWidth(), DesignR.drawable.icon_arrow_up_right)
                } }
            }
        }
    }
}
