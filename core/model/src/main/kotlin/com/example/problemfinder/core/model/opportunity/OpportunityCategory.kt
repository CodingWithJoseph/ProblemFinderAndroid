package com.example.problemfinder.core.model.opportunity

enum class OpportunityCategory {
    BUILD_AND_TOOLING,
    DATA_AND_APIS,
    WORKFLOW_AND_AUTOMATION,
    UNCATEGORIZED
}


fun String.toOpportunityCategory(): OpportunityCategory =
    when (this) {
        "build_and_tooling" -> OpportunityCategory.BUILD_AND_TOOLING
        "data_and_apis" -> OpportunityCategory.DATA_AND_APIS
        "workflow_and_automation" -> OpportunityCategory.WORKFLOW_AND_AUTOMATION
        "uncategorized" -> OpportunityCategory.UNCATEGORIZED
        else -> throw IllegalArgumentException("Unknown opportunity category: $this")
    }
