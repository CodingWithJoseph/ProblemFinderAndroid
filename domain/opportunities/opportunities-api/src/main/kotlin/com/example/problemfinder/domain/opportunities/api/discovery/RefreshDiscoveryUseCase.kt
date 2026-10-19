package com.example.problemfinder.domain.opportunities.api.discovery

import com.example.problemfinder.core.common.UseCase

/** Fetches and stores the current account's dashboard; observers receive the updated cache. */
abstract class RefreshDiscoveryUseCase : UseCase<Unit, Unit>()
