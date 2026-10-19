package com.example.problemfinder.feature.projects.impl

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.problemfinder.core.designsystem.theme.ImmensityTheme


@Preview(
    name = "Compact",
    widthDp = 360,
    heightDp = 640
)
@Preview(
    name = "Compact Dark",
    widthDp = 360,
    heightDp = 640,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Preview(
    name = "Compact Landscape",
    device = "spec:width=360dp,height=640dp,dpi=420,isRound=false,chinSize=0dp,orientation=landscape"
)
@Composable
private fun ProjectsScreenCompactPreview() {
    ImmensityTheme {
        ProjectsScreen()
    }
}

@Preview(
    name = "Tall",
    widthDp = 393,
    heightDp = 850
)
@Preview(
    name = "Tall Dark",
    widthDp = 393,
    heightDp = 850,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Preview(
    name = "Tall Landscape",
    device = "spec:width=393dp,height=850dp,dpi=420,isRound=false,chinSize=0dp,orientation=landscape"
)
@Composable
private fun ProjectsScreenStandardPreview() = ImmensityTheme {
    ProjectsScreen()
}


/** Placeholder tab content; the title does not represent an implemented projects workflow. */
@Composable
fun ProjectsScreen(
    modifier: Modifier = Modifier,
) = Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {

        Text("Projects")

    }
}
