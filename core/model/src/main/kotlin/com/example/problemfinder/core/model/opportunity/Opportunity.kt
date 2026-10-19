package com.example.problemfinder.core.model.opportunity

import kotlin.time.Instant

/**
 * Validated app model for a backend cluster. isWatched belongs to the account reading it, while
 * date may be absent. Network/database mappers must satisfy these invariants before UI sees it.
 */
data class Opportunity(
    val id: Int,
    val name: String,
    val summary: String,
    val problemStatement: String,
    val postCount: Int,
    val isWatched: Boolean,
    val date: Instant?,
    val category: OpportunityCategory = OpportunityCategory.UNCATEGORIZED
) {
    init {
        require(id > 0) { "Opportunity ID must be positive" }
        require(name.isNotBlank()) { "Opportunity name must not be blank" }
        require(summary.isNotBlank()) { "Opportunity summary must not be blank" }
        require(problemStatement.isNotBlank()) { "Opportunity problem statement must not be blank" }
        require(postCount >= 0) { "Opportunity post count must not be negative" }
    }
}
