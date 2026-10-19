package com.example.problemfinder.data.opportunities.impl.util

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex

/**
 * Serializes network reads plus cache writes with mutations: a refresh that started before
 * Add/Remove must not overwrite its successful result with stale membership. The app-wide lock
 * covers the request and its write, not just Room access; cache observers do not acquire it.
 */
@Singleton
class OpportunitySyncCoordinator @Inject constructor() {
    val mutex = Mutex()
}
