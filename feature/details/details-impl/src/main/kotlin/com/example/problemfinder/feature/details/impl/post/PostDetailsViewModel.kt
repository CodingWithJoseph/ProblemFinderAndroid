package com.example.problemfinder.feature.details.impl.post

import androidx.lifecycle.SavedStateHandle
import com.example.problemfinder.core.mvi.BaseMVIViewModel
import com.example.problemfinder.feature.details.api.OpportunityDetailsDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** Navigation plumbing only. Implement the selected post's data lifecycle yourself. */
@HiltViewModel
class PostDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : BaseMVIViewModel<PostDetailsIntent, PostDetailsState, PostDetailsEffect>(
    PostDetailsState(
        clusterId = savedStateHandle[OpportunityDetailsDestination.ID_ARGUMENT],
        postId = savedStateHandle[OpportunityDetailsDestination.POST_ARGUMENT],
    )
) {
    override fun onIntent(intent: PostDetailsIntent) {
        when (intent) {
            PostDetailsIntent.Back -> sendEffect(PostDetailsEffect.NavigateBack)
            is PostDetailsIntent.OpenOriginal -> sendEffect(PostDetailsEffect.OpenExternal(intent.url))
            PostDetailsIntent.Entered, PostDetailsIntent.Retry -> {
                // TODO [POST-LOGIC] Observe this account/cluster using the existing Details use case.
                // Select postId from loaded evidence, publish post/status, and handle missing posts.
                // Restore using route IDs after process recreation; no object is a nav argument.
            }
        }
    }
    override fun onError(error: Throwable) {
        // TODO [POST-LOGIC] Map observation failure to presentation state and make Retry recoverable.
    }
}
