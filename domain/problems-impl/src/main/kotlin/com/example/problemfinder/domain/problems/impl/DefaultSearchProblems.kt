package com.example.problemfinder.domain.problems.impl

import com.example.problemfinder.core.model.Problem
import com.example.problemfinder.data.problems.api.ProblemRepository
import com.example.problemfinder.domain.problems.api.SearchProblems
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Use-case placeholder. Keep orchestration and business policy here; keep I/O in the data layer.
 */
class DefaultSearchProblems @Inject constructor(
    private val repository: ProblemRepository,
) : SearchProblems {
    override fun invoke(query: String): Flow<List<Problem>> =
        TODO("Implement validation, search policy, and repository delegation")
}
