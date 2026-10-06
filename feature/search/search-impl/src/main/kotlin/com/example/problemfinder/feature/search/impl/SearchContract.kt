package com.example.problemfinder.feature.search.impl



data class SearchState(val placeholder: String = "")

sealed interface SearchIntent

sealed interface SearchEffect
