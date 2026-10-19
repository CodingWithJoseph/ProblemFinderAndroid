package com.example.problemfinder.core.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException


/**
 * Shared coroutine and stream plumbing; each feature owns its intents and state transitions.
 * StateFlow exposes the latest state to collectors. The buffered channel queues effects for a
 * consumer while this ViewModel lives; effects are neither broadcast nor persisted across death.
 */
abstract class BaseMVIViewModel<Intent, State, Effect>(initialState: State) : ViewModel(),
    MVIStore<Intent, State, Effect> {

    private val mutableState = MutableStateFlow(initialState)
    private val effectChannel = Channel<Effect>(capacity = Channel.BUFFERED)

    final override val state: StateFlow<State> = mutableState.asStateFlow()
    final override val effects: Flow<Effect> = effectChannel.receiveAsFlow()


    /** Runs in viewModelScope, forwarding ordinary exceptions to [onError] but preserving cancellation. */
    protected fun safeLaunch(block: suspend CoroutineScope.() -> Unit): Job =
        viewModelScope.launch {
            try {
                block()
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                onError(error)
            }
        }

    /** Atomically updates state. Keep the transform free of side effects: update may retry it. */
    protected fun reduce(transform: State.() -> State) {
        mutableState.update(transform)
    }

    /** Enqueues a transient UI command without blocking the caller; delivery needs an active consumer. */
    protected fun sendEffect(effect: Effect) {
        viewModelScope.launch {
            effectChannel.send(effect)
        }
    }

    protected abstract fun onError(error: Throwable)
}

