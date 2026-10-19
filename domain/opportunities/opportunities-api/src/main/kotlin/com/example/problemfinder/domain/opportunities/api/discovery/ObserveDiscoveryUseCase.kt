package com.example.problemfinder.domain.opportunities.api.discovery

import com.example.problemfinder.core.common.ObservableUseCase
import com.example.problemfinder.core.model.discovery.DiscoverySnapshot

abstract class ObserveDiscoveryUseCase: ObservableUseCase<Unit, DiscoverySnapshot>()
