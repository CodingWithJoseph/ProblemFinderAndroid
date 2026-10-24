package com.example.problemfinder.data.opportunities.impl.model.entity

import androidx.room3.Embedded

data class OpportunityDetailsCacheRow (
    @Embedded(prefix = "details_")
    val details: OpportunityDetailsEntity,

    @Embedded(prefix = "opportunity_")
    val opportunity: OpportunityEntity?,

    @Embedded(prefix = "membership_")
    val membership: OpportunityEvidenceMembershipEntity?,

    @Embedded(prefix = "evidence_")
    val evidence: OpportunityEvidenceEntity?
)

