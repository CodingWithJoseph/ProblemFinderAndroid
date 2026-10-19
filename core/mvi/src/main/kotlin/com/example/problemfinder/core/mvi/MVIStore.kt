package com.example.problemfinder.core.mvi

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * UI boundary for Model-View-Intent: actions enter through [onIntent], durable render data is
 * observed through [state], and transient commands such as navigation leave through [effects].
 */
interface MVIStore<in Intent, out State, out Effect> {
    val state: StateFlow<State>
    val effects: Flow<Effect>
    fun onIntent(intent: Intent)
}

