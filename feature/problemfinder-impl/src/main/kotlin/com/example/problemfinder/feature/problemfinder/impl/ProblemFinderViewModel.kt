package com.example.problemfinder.feature.problemfinder.impl

import com.example.problemfinder.core.mvi.MviViewModel
import com.example.problemfinder.domain.problems.api.SearchProblems
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ProblemFinderViewModel @Inject constructor(
    private val searchProblems: SearchProblems,
) : MviViewModel<ProblemFinderIntent, ProblemFinderState, ProblemFinderEffect>(
    initialState = ProblemFinderState(),
) {
    override fun onIntent(intent: ProblemFinderIntent) {
        // TODO: Implement the reducer/effect policy for each intent.
        @Suppress("UNUSED_VARIABLE")
        val intentionallyUnimplementedIntent = intent
        @Suppress("UNUSED_VARIABLE")
        val intentionallyReservedUseCase = searchProblems
        TODO("Implement ProblemFinderViewModel")
    }
}
