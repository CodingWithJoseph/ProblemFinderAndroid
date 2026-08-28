package com.example.problemfinder.data.problems.api

import com.example.problemfinder.core.model.Problem
import kotlinx.coroutines.flow.Flow

/** Stable seam between domain code and all concrete data sources. */
interface ProblemRepository {
    fun search(query: String): Flow<List<Problem>>
}

