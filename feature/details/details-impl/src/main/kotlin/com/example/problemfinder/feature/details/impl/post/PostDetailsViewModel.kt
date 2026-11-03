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
                // REVIEW [DETAILS-14] This branch currently never loads anything. Starting with
                // the two route IDs already in state, sketch: observe -> select evidence -> publish.
                // Which existing use case supplies account-scoped snapshots, and which two lists
                // can contain this post? How will repeated Entered avoid a duplicate collector?
                // Check: a tapped cached post displays its full body; an account change clears it;
                // Retry can restart a failed collector, and route IDs still work after recreation.
                //
                // REVIEW [DETAILS-15] Missing versus not loaded: cold-open a post that originally
                // came from page 3, with no saved evidence. Refreshing page 1 may succeed without it.
                // What does that absence actually prove? Trace cache-not-ready, a failed fetch,
                // and a successfully loaded page that simply does not include the selected ID.
                // Decide the recovery/lookup contract before displaying a definitive missing state;
                // neither a preview fixture nor a second copy of the details object solves lookup.
            }
        }
    }
    override fun onError(error: Throwable) {
        // REVIEW [DETAILS-14] What should a failure change when post is already present, versus
        // before it has ever loaded? Trace PostDetailsScreen's two branches, and keep failures from
        // exposing a previous account's post. Retry needs to restore the failed work, not just a label.
    }
}
