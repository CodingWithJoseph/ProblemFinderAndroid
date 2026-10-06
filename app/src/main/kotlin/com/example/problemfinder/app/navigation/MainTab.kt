package com.example.problemfinder.app.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.problemfinder.app.R
import com.example.problemfinder.feature.browse.api.BrowseDestination
import com.example.problemfinder.feature.builds.api.BuildsDestination
import com.example.problemfinder.feature.projects.api.ProjectsDestination
import com.example.problemfinder.feature.search.api.SearchDestination

enum class MainTab(
    val route: String,
    @StringRes val label: Int,
    @DrawableRes val icon: Int
) {
    BROWSE(
        route = BrowseDestination.ROUTE,
        label = R.string.tab_label_browse,
        icon = R.drawable.icon_browse
    ),

    SEARCH(
        route = SearchDestination.ROUTE,
        label = R.string.tab_label_search,
        icon = R.drawable.icon_search
    ),

    PROJECTS(
        route = ProjectsDestination.ROUTE,
        label = R.string.tab_label_projects,
        icon = R.drawable.icon_projects
    ),

    BUILDS(
        route = BuildsDestination.ROUTE,
        label = R.string.tab_label_builds,
        icon = R.drawable.icon_builds
    )
}
