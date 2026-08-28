package com.example.problemfinder.core.model

@JvmInline
value class ProblemId(val value: String)

/**
 * The deliberately minimal model shared across architectural boundaries.
 *
 * TODO: Add only product-level fields. Keep transport and persistence details in data implementations.
 */
data class Problem(
    val id: ProblemId,
)

