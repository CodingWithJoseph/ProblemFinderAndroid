package com.example.problemfinder.data.problems.impl

import com.example.problemfinder.core.model.Problem
import com.example.problemfinder.data.problems.api.ProblemRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Composition-ready repository placeholder.
 *
 * TODO: Inject concrete local/remote sources, choose source-of-truth policy, map models, and expose Flow.
 */
class DefaultProblemRepository @Inject constructor() : ProblemRepository {
    override fun search(query: String): Flow<List<Problem>> =
        TODO("Implement data sources and mapping")
}
