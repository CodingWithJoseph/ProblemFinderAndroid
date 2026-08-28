package com.example.problemfinder.core.common.coroutines

import kotlinx.coroutines.CoroutineDispatcher

/**
 * Dispatcher policy seam. Bind a production implementation in the composition root and use a test
 * implementation in unit tests.
 */
interface AppDispatchers {
    val default: CoroutineDispatcher
    val io: CoroutineDispatcher
    val main: CoroutineDispatcher
    val unconfined: CoroutineDispatcher
}

