package com.example.problemfinder.core.designsystem.theme

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.example.problemfinder.core.designsystem.R
import com.example.problemfinder.core.model.opportunity.OpportunityCategory


data class OpportunityAppearance(
    @StringRes val title: Int,
    @StringRes val description: Int,
    @DrawableRes val icon: Int,
    val backgroundColor: Color,
    val foregroundColor: Color,
)

/** Maps domain categories to localized labels, icons, and colors without putting UI types in models. */
fun OpportunityCategory.appearance(darkTheme: Boolean = false): OpportunityAppearance {
    val foreground = if (darkTheme) Color(0xFFF4F4EC) else Color(0xFF242621)
    return when (this) {
        OpportunityCategory.BUILD_AND_TOOLING -> OpportunityAppearance(
            title = R.string.label_build_tooling,
            description = R.string.description_build_tooling,
            icon = R.drawable.icon_terminal,
            backgroundColor = if (darkTheme) Color(0xFF39402C) else Color(0xFFE6E9D7),
            foregroundColor = foreground,
        )
        OpportunityCategory.DATA_AND_APIS -> OpportunityAppearance(
            title = R.string.label_data_apis,
            description = R.string.description_data_apis,
            icon = R.drawable.icon_database,
            backgroundColor = if (darkTheme) Color(0xFF393348) else Color(0xFFE7E3F2),
            foregroundColor = foreground,
        )
        OpportunityCategory.WORKFLOW_AND_AUTOMATION -> OpportunityAppearance(
            title = R.string.label_workflow_automation,
            description = R.string.description_workflow_automation,
            icon = R.drawable.icon_workflow,
            backgroundColor = if (darkTheme) Color(0xFF48352A) else Color(0xFFF5E4D8),
            foregroundColor = foreground,
        )
        OpportunityCategory.UNCATEGORIZED -> OpportunityAppearance(
            title = R.string.label_uncategorized,
            description = R.string.description_uncategorized,
            icon = R.drawable.icon_uncategorized,
            backgroundColor = if (darkTheme) Color(0xFF353630) else Color(0xFFE8E8E0),
            foregroundColor = foreground,
        )
    }
}
