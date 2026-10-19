package com.example.problemfinder.domain.opportunities.api.discovery

import com.example.problemfinder.core.common.ObservableUseCase
import com.example.problemfinder.core.model.discovery.DiscoverySnapshot

/** Observes the current account's cached dashboard independently of network refresh requests. */
abstract class ObserveDiscoveryUseCase: ObservableUseCase<Unit, DiscoverySnapshot>()
