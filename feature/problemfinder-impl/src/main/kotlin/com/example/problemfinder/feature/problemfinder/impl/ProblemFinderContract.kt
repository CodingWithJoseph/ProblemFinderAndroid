package com.example.problemfinder.feature.problemfinder.impl

import com.example.problemfinder.core.model.Problem
import com.example.problemfinder.core.model.ProblemId

sealed interface ProblemFinderIntent {
    data class QueryChanged(val query: String) : ProblemFinderIntent
    data object SearchSubmitted : ProblemFinderIntent
    data object Retry : ProblemFinderIntent
    data class ProblemSelected(val problemId: ProblemId) : ProblemFinderIntent
}

data class ProblemFinderState(
    val query: String = "",
    val problems: List<Problem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface ProblemFinderEffect {
    data class OpenProblem(val problemId: ProblemId) : ProblemFinderEffect
    data class ShowMessage(val message: String) : ProblemFinderEffect
}

