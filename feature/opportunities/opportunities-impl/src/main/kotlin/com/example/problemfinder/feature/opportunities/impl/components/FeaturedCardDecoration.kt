package com.example.problemfinder.feature.opportunities.impl.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.example.problemfinder.core.designsystem.theme.DiscoverySpotlightColors

@Composable
internal fun FeaturedCardDecoration(
    @DrawableRes iconRes: Int,
    modifier: Modifier = Modifier,
) {
    val iconTileShape = RoundedCornerShape(19.dp)

    Box(modifier = modifier.clearAndSetSemantics {}) {
        Canvas(Modifier.matchParentSize()) {
            val center = Offset(size.width - 40.dp.toPx(), 65.dp.toPx())
            val stroke = Stroke(width = 1.dp.toPx())
            for (radius in listOf(120.dp, 85.dp, 50.dp)) {
                drawCircle(
                    color = DiscoverySpotlightColors.ring,
                    radius = radius.toPx(),
                    center = center,
                    style = stroke,
                )
            }
        }

        Box(
            modifier = Modifier.align(Alignment.TopEnd)
                .padding(top = 33.dp, end = 48.dp)
                .size(67.dp)
                .rotate(-12f)
                .shadow(8.dp, iconTileShape, clip = false)
                .background(DiscoverySpotlightColors.accent, iconTileShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = DiscoverySpotlightColors.surface,
                modifier = Modifier.size(32.dp),
            )
        }
    }
}
