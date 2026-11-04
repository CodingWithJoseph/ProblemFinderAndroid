package com.example.problemfinder.feature.details.impl.post

import androidx.lifecycle.SavedStateHandle
import com.example.problemfinder.core.designsystem.components.FeatureContentStatus
import com.example.problemfinder.core.model.opportunity.OpportunityFailure
import com.example.problemfinder.core.model.opportunity.OpportunityOperationException
import com.example.problemfinder.core.mvi.BaseMVIViewModel
import com.example.problemfinder.domain.opportunities.api.details.LoadOpportunityPostUseCase
import com.example.problemfinder.domain.opportunities.api.details.ObserveOpportunityDetailsUseCase
import com.example.problemfinder.feature.details.api.OpportunityDetailsDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class PostDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeDetails: ObserveOpportunityDetailsUseCase,
    private val loadPost: LoadOpportunityPostUseCase,
) : BaseMVIViewModel<PostDetailsIntent, PostDetailsState, PostDetailsEffect>(
    PostDetailsState(
        clusterId = savedStateHandle[OpportunityDetailsDestination.ID_ARGUMENT],
        postId = savedStateHandle[OpportunityDetailsDestination.POST_ARGUMENT],
        status = FeatureContentStatus.LOADING,
    )
) {
    private var observation: Job? = null

    override fun onIntent(intent: PostDetailsIntent) {
        when (intent) {
            PostDetailsIntent.Back -> sendEffect(PostDetailsEffect.NavigateBack)
            is PostDetailsIntent.OpenOriginal -> sendEffect(PostDetailsEffect.OpenExternal(intent.url))
            PostDetailsIntent.Entered -> if (observation?.isActive != true) observe() else Unit
            PostDetailsIntent.Retry -> observe()
        }
    }

    private fun observe() {
        val cluster = state.value.clusterId ?: return
        val id = state.value.postId ?: return
        observation?.cancel()
        reduce { copy(status = FeatureContentStatus.LOADING) }
        observation = safeLaunch {
            observeDetails(ObserveOpportunityDetailsUseCase.Params(cluster))
                .map { snapshot ->
                    Triple(snapshot.userId, snapshot.cacheReady,
                        snapshot.posts.firstOrNull { it.id == id } ?: snapshot.topEvidence.firstOrNull { it.id == id })
                }
                .distinctUntilChanged()
                .collectLatest { (user, ready, cached) ->
                    reduce { copy(post = cached, status = FeatureContentStatus.LOADING) }
                    if (!ready) return@collectLatest
                    if (user == null || cached != null) {
                        reduce { copy(status = if (cached == null) FeatureContentStatus.EMPTY else FeatureContentStatus.CONTENT) }
                        return@collectLatest
                    }
                    try {
                        val post = loadPost(LoadOpportunityPostUseCase.Params(cluster, id, user))
                        reduce { copy(post = post, status = if (post == null) FeatureContentStatus.EMPTY else FeatureContentStatus.CONTENT) }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        val missing = (error as? OpportunityOperationException)?.failure == OpportunityFailure.CLUSTER_NOT_FOUND
                        reduce { copy(status = if (missing) FeatureContentStatus.EMPTY else FeatureContentStatus.ERROR) }
                    }
                }
        }
    }

    override fun onError(error: Throwable) {
        reduce { copy(status = FeatureContentStatus.ERROR) }
    }
}
