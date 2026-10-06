package com.example.problemfinder.feature.builds.impl


data class BuildsState(
    val email: String = ""
)

sealed interface BuildsIntent
sealed interface BuildsEffect
