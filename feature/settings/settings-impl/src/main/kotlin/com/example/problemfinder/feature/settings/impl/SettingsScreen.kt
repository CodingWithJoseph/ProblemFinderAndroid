package com.example.problemfinder.feature.settings.impl

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.problemfinder.core.designsystem.theme.ImmensityTheme
import com.example.problemfinder.feature.settings.impl.R
import com.example.problemfinder.core.designsystem.R as DesignSystemR

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
private fun SettingsScreenCompactPreview() {
    ImmensityTheme {
        SettingsScreen(onClose = {})
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
private fun SettingsScreenPreview() = ImmensityTheme {
    SettingsScreen(onClose = {})
}


@Composable
fun SettingsScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) = Surface(
    modifier = modifier
        .safeDrawingPadding()
        .fillMaxSize(),
    color = MaterialTheme.colorScheme.background,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().padding(24.dp)
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {

            Text(
                text = stringResource(R.string.feature_name),
                style = MaterialTheme.typography.headlineSmall,
            )

            IconButton(onClick = onClose) {
                Icon(
                    painter = painterResource(DesignSystemR.drawable.icon_close),
                    contentDescription = stringResource(DesignSystemR.string.icon_close)
                )
            }
        }

    }
}
