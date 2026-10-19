package com.example.problemfinder.feature.authentication.impl

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.problemfinder.core.designsystem.theme.ImmensityColors
import com.example.problemfinder.core.designsystem.theme.ImmensityTheme
import com.example.problemfinder.core.designsystem.R as DesignSystemR


@Composable
fun AuthLogo() = Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(DesignSystemR.drawable.logo_orange),
            contentDescription = stringResource(DesignSystemR.string.logo_immensity),
            modifier = Modifier.size(22.dp)
        )

        Spacer(Modifier.width(6.dp))

        Text(
            stringResource(DesignSystemR.string.wordmark_immensity),
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium
        )
    }

@Preview
@Composable
private fun AuthLogoPreview() = ImmensityTheme {
    AuthLogo()
}

@Preview
@Composable
fun AuthMailIcon(padding: Dp = 20.dp) {
    Icon(
        painter = painterResource(DesignSystemR.drawable.icon_mail),
        contentDescription = null,
        tint = ImmensityColors.Orange,
        modifier = Modifier
            .size(24.dp + padding * 2)
            .background(ImmensityColors.Orange.copy(alpha = 0.10f), shape = RoundedCornerShape(25))
            .padding(padding),
    )
}

