package com.example.wl.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import com.example.wl.ui.theme.GradientEnd
import com.example.wl.ui.theme.GradientMid
import com.example.wl.ui.theme.GradientStart

@Composable
fun AnimatedGradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "gradient")
    val fraction by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradientFraction"
    )

    Box(
        modifier = modifier.background(
            Brush.linearGradient(
                colors = listOf(
                    GradientStart,
                    GradientMid,
                    GradientEnd,
                    GradientMid,
                    GradientStart
                ),
                start = Offset(0f, 0f),
                end = Offset(
                    with(LocalDensity.current) { LocalContext.current.resources.displayMetrics.widthPixels.toFloat() * fraction },
                    with(LocalDensity.current) { LocalContext.current.resources.displayMetrics.heightPixels.toFloat() * fraction }
                )
            )
        )
    ) {
        content()
    }
}
