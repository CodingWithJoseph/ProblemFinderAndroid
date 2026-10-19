package com.example.problemfinder.domain.opportunities.impl.discovery

import com.example.problemfinder.data.opportunities.api.DiscoveryRepository
import com.example.problemfinder.domain.opportunities.api.discovery.RefreshDiscoveryUseCase
import javax.inject.Inject

class RefreshDiscoveryUseCaseImpl @Inject constructor(private val repository: DiscoveryRepository) : RefreshDiscoveryUseCase() {
    override suspend fun execute(params: Unit) = repository.refresh()
}
