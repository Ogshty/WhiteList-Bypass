package com.example.wl.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.wl.R

@Composable
fun PulseFloatingActionButton(
    isConnected: Boolean,
    isConnecting: Boolean,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val containerColor by animateColorAsState(
        targetValue = when {
            isConnecting -> MaterialTheme.colorScheme.tertiaryContainer
            isConnected -> MaterialTheme.colorScheme.errorContainer
            else -> MaterialTheme.colorScheme.primaryContainer
        },
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "fabColor"
    )

    val contentColor by animateColorAsState(
        targetValue = when {
            isConnecting -> MaterialTheme.colorScheme.onTertiaryContainer
            isConnected -> MaterialTheme.colorScheme.onErrorContainer
            else -> MaterialTheme.colorScheme.onPrimaryContainer
        },
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "fabContentColor"
    )

    val shape = MaterialTheme.shapes.extraLarge

    val fabAlpha = if (isConnecting) glowAlpha else 0f
    Box(
        modifier = Modifier
            .scale(if (isConnecting) pulseScale else 1f)
            .graphicsLayer {
                // Glow effect
                shadowElevation = if (isConnecting) 24f else 8f
                ambientShadowColor = primaryColor.copy(alpha = fabAlpha)
                spotShadowColor = primaryColor.copy(alpha = fabAlpha)
            }
    ) {
        ExtendedFloatingActionButton(
            onClick = onClick,
            containerColor = containerColor,
            contentColor = contentColor,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 0.dp,
                pressedElevation = 4.dp
            ),
            shape = shape,
            icon = {
                Crossfade(targetState = isConnecting, animationSpec = tween(400), label = "fabIcon") { connecting ->
                    if (connecting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Icon(
                            if (isConnected) Icons.Default.PowerSettingsNew else Icons.Default.Bolt,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            },
            text = {
                val labelText = when {
                    isConnecting -> stringResource(R.string.status_connecting)
                    isConnected -> stringResource(R.string.disconnect)
                    else -> stringResource(R.string.connect)
                }
                Text(
                    text = labelText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        )
    }
}
