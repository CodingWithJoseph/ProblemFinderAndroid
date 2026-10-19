package com.example.problemfinder.data.opportunities.impl.util

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex

// Serialize network reads + cache writes with mutations: a refresh that started
// before Add/Remove must not overwrite its successful result with stale membership.
@Singleton
class OpportunitySyncCoordinator @Inject constructor() {
    val mutex = Mutex()
}
