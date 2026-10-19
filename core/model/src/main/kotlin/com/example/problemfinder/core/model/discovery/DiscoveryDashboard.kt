package com.example.problemfinder.core.model.discovery

import com.example.problemfinder.core.model.opportunity.OpportunityCategory

import com.example.problemfinder.core.model.opportunity.Opportunity


data class DiscoveryDashboard(
    val featuredCluster: Opportunity?,
    val collections: List<DiscoveryCollection>,
)

data class DiscoveryCollection(
    val category: OpportunityCategory,
    val clusters: List<Opportunity>,
)
