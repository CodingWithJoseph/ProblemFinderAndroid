package com.example.problemfinder.app.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.problemfinder.app.R
import com.example.problemfinder.core.designsystem.R as DesignSystemR
import com.example.problemfinder.feature.opportunities.api.OpportunitiesDestinations
import com.example.problemfinder.feature.builds.api.BuildsDestination
import com.example.problemfinder.feature.projects.api.ProjectsDestination
import com.example.problemfinder.feature.search.api.SearchDestination

/** Top-level tab metadata; Discovery also represents the nested Browse destination. */
enum class MainTab(
    val route: String,
    @StringRes val label: Int,
    @DrawableRes val icon: Int
) {
    DISCOVERY(
        route = OpportunitiesDestinations.DISCOVERY,
        label = R.string.tab_label_browse,
        icon = DesignSystemR.drawable.icon_browse
    ),

    SEARCH(
        route = SearchDestination.ROUTE,
        label = R.string.tab_label_search,
        icon = DesignSystemR.drawable.icon_search
    ),

    PROJECTS(
        route = ProjectsDestination.ROUTE,
        label = R.string.tab_label_projects,
        icon = DesignSystemR.drawable.icon_projects
    ),

    BUILDS(
        route = BuildsDestination.ROUTE,
        label = R.string.tab_label_builds,
        icon = DesignSystemR.drawable.icon_builds
    )
}
