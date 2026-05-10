package com.example.wl.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun AnimatedModeSelector(
    currentMode: String,
    onModeSelected: (String) -> Unit
) {
    val modes = listOf("whitelist", "tor", "dpi")
    val modeLabels = listOf("White List", "Orbot Tor", "Bye Bye DPI")

    val indicatorIndex = modes.indexOf(currentMode).coerceAtLeast(0)
    val animProgress by animateFloatAsState(
        targetValue = indicatorIndex.toFloat(),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "indicator"
    )

    val itemWidth = 110.dp
    val density = LocalDensity.current
    val itemWidthPx = with(density) { itemWidth.toPx() }
    val offset = animProgress * itemWidthPx

    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .padding(6.dp)
        ) {
            // Sliding indicator
            Box(
                modifier = Modifier
                    .offset(x = with(density) { offset.toDp() })
                    .width(itemWidth)
                    .height(48.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.primaryContainer)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                modes.forEachIndexed { index, mode ->
                    val isSelected = currentMode == mode
                    Text(
                        text = modeLabels[index],
                        modifier = Modifier
                            .width(itemWidth)
                            .height(48.dp)
                            .clickable { onModeSelected(mode) }
                            .wrapContentSize(Alignment.Center),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
