package com.example.problemfinder.domain.problems.api

import com.example.problemfinder.core.model.Problem
import kotlinx.coroutines.flow.Flow

/** Feature-facing use-case abstraction. */
fun interface SearchProblems {
    operator fun invoke(query: String): Flow<List<Problem>>
}

