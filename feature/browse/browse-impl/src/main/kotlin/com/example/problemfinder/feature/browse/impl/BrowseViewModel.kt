package com.example.problemfinder.feature.browse.impl

import com.example.problemfinder.core.mvi.BaseMVIViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class BrowseViewModel @Inject constructor(): BaseMVIViewModel<BrowseIntent, BrowseState, BrowseEffect>(BrowseState()){
    override fun onIntent(intent: BrowseIntent) {}
}
