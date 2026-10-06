package com.example.problemfinder.feature.browse.impl


data class BrowseState(
    val email: String = "",
)

sealed interface BrowseIntent

sealed interface BrowseEffect
