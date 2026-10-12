package com.example.problemfinder.feature.search.impl

import com.example.problemfinder.core.mvi.BaseMVIViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(): BaseMVIViewModel<SearchIntent, SearchState, SearchEffect>(SearchState()){
    override fun onIntent(intent: SearchIntent) {}
    override fun onError(error: Throwable) {}
}
