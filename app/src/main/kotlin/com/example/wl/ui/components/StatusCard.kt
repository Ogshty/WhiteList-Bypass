package com.example.wl.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.wl.R
import com.example.wl.SubscriptionViewModel
import com.example.wl.data.ProxyConfig
import com.example.wl.ui.theme.StatusConnected
import com.example.wl.ui.theme.StatusConnecting
import com.example.wl.ui.theme.StatusDisconnected

@Composable
fun AnimatedStatusCard(
    viewModel: SubscriptionViewModel,
    connectionState: SubscriptionViewModel.ConnectionState,
    selectedProxy: ProxyConfig?,
    connectionMode: String = "whitelist"
) {
    val isConnected = connectionState == SubscriptionViewModel.ConnectionState.CONNECTED
    val isConnecting = connectionState == SubscriptionViewModel.ConnectionState.CONNECTING
    val torProgress = viewModel.torProgress

    // Premium Material 3 look uses tonal containers for status
    val containerColor by animateColorAsState(
        targetValue = when {
            isConnected -> MaterialTheme.colorScheme.primaryContainer
            isConnecting -> MaterialTheme.colorScheme.secondaryContainer
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = tween(500),
        label = "cardColor"
    )

    val contentColor by animateColorAsState(
        targetValue = when {
            isConnected -> MaterialTheme.colorScheme.onPrimaryContainer
            isConnecting -> MaterialTheme.colorScheme.onSecondaryContainer
            else -> MaterialTheme.colorScheme.onSurface
        },
        animationSpec = tween(500),
        label = "contentColor"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(28.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = when {
                        isConnected -> stringResource(R.string.status_connected)
                        isConnecting -> stringResource(R.string.status_connecting)
                        else -> stringResource(R.string.status_disconnected)
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )

                AnimatedVisibility(
                    visible = isConnecting && connectionMode == "tor" && torProgress > 0,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        LinearProgressIndicator(
                            progress = { torProgress / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(CircleShape),
                            color = contentColor,
                            trackColor = contentColor.copy(alpha = 0.1f)
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(contentColor.copy(alpha = 0.08f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                val dotColor = when {
                    isConnected -> StatusConnected
                    isConnecting -> StatusConnecting
                    else -> StatusDisconnected
                }

                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = when {
                        isConnected -> {
                            when (connectionMode) {
                                "tor" -> "Tor Network"
                                "dpi" -> "Bye Bye DPI"
                                else -> selectedProxy?.name ?: stringResource(R.string.traffic_routed)
                            }
                        }
                        isConnecting -> {
                            if (connectionMode == "tor" && torProgress > 0) "Bootstrapping Tor: $torProgress%"
                            else stringResource(R.string.finding_server)
                        }
                        else -> stringResource(R.string.tap_to_connect)
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = contentColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}
