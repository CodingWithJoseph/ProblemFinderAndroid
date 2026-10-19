package com.example.problemfinder.domain.opportunities.impl.discovery

import com.example.problemfinder.core.model.discovery.DiscoverySnapshot
import com.example.problemfinder.data.opportunities.api.DiscoveryRepository
import com.example.problemfinder.domain.opportunities.api.discovery.ObserveDiscoveryUseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveDiscoveryUseCaseImpl @Inject constructor(
    private val repository: DiscoveryRepository
) : ObserveDiscoveryUseCase() {
    override suspend fun execute(params: Unit): Flow<DiscoverySnapshot> = repository.observeDiscovery()
}
