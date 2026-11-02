package com.example.problemfinder.feature.details.impl.post

import com.example.problemfinder.core.designsystem.components.FeatureContentStatus
import com.example.problemfinder.core.model.details.OpportunityEvidence

data class PostDetailsState(
    val clusterId: Int? = null,
    val postId: Int? = null,
    val post: OpportunityEvidence? = null,
    val status: FeatureContentStatus = FeatureContentStatus.EMPTY,
)

sealed interface PostDetailsIntent {
    data object Entered : PostDetailsIntent
    data object Retry : PostDetailsIntent
    data object Back : PostDetailsIntent
    data class OpenOriginal(val url: String) : PostDetailsIntent
}

sealed interface PostDetailsEffect {
    data object NavigateBack : PostDetailsEffect
    data class OpenExternal(val url: String) : PostDetailsEffect
}
