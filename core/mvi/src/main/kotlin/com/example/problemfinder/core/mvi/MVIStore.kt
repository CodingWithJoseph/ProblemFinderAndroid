package com.example.problemfinder.core.mvi

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface MVIStore<in Intent, out State, out Effect> {
    val state: StateFlow<State>
    val effects: Flow<Effect>
    fun onIntent(intent: Intent)
}

