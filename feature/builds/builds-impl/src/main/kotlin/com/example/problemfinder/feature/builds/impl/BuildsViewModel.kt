package com.example.problemfinder.feature.builds.impl

import com.example.problemfinder.core.mvi.BaseMVIViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class BuildsViewModel @Inject constructor(): BaseMVIViewModel<BuildsIntent, BuildsState, BuildsEffect>(BuildsState()){
    override fun onIntent(intent: BuildsIntent) {}
}
