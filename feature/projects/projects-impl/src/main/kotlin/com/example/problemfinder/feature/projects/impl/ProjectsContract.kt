package com.example.problemfinder.feature.projects.impl


data class ProjectsState(val email: String = "", )

sealed interface ProjectsIntent

sealed interface ProjectsEffect
