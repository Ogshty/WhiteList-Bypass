package com.example.wl.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun AnimatedSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val thumbColor by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(200),
        label = "thumbColor"
    )
    val trackColor by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
        animationSpec = tween(200),
        label = "trackColor"
    )

    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = thumbColor,
            checkedTrackColor = trackColor,
            uncheckedThumbColor = thumbColor,
            uncheckedTrackColor = trackColor
        )
    )
}

@Composable
fun DpiSettingsDialog(
    currentSize: String,
    currentSleep: String,
    currentPacketType: String,
    currentHttpFragment: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, Boolean) -> Unit,
    onReset: () -> Unit
) {
    var size by remember { mutableStateOf(currentSize) }
    var sleep by remember { mutableStateOf(currentSleep) }
    var packetType by remember { mutableStateOf(currentPacketType) }
    var httpFragment by remember { mutableStateOf(currentHttpFragment) }

    LaunchedEffect(currentSize, currentSleep, currentPacketType, currentHttpFragment) {
        size = currentSize
        sleep = currentSleep
        packetType = currentPacketType
        httpFragment = currentHttpFragment
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("DPI Fragmentation Settings", style = MaterialTheme.typography.headlineSmall) },
        shape = MaterialTheme.shapes.extraLarge,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = size,
                    onValueChange = { size = it },
                    label = { Text("Size Range (e.g. 1-500)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )
                OutlinedTextField(
                    value = sleep,
                    onValueChange = { sleep = it },
                    label = { Text("Sleep Range (e.g. 0-500)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Fragment HTTP",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    AnimatedSwitch(checked = httpFragment, onCheckedChange = { httpFragment = it })
                }

                Text(
                    text = "Fragmentation Target",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = packetType == "tlshello",
                        onClick = { packetType = "tlshello" }
                    )
                    Text(
                        "TLS ClientHello only (Stable)",
                        modifier = Modifier.clickable { packetType = "tlshello" }
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = packetType == "all",
                        onClick = { packetType = "all" }
                    )
                    Text(
                        "All Packets (Aggressive)",
                        modifier = Modifier.clickable { packetType = "all" }
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onReset) {
                    Text("Reset")
                }
                Button(onClick = { onConfirm(size, sleep, packetType, httpFragment) }) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}

@Composable
fun TorBridgeDialog(currentType: String, onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    val options = listOf(
        "none" to "None (Direct)",
        "snowflake" to "Snowflake (Anti-censorship)",
        "obfs4" to "Obfs4 (Obfuscated)"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Tor Bridge", style = MaterialTheme.typography.headlineSmall) },
        shape = MaterialTheme.shapes.extraLarge,
        text = {
            Column {
                options.forEach { (type, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(type) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = currentType == type, onClick = { onSelect(type) })
                        Spacer(Modifier.width(8.dp))
                        Text(label)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

fun LazyListScope.settingsItem(
    headline: @Composable () -> Unit,
    supporting: @Composable () -> Unit,
    trailing: @Composable (() -> Unit)? = null,
    leading: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    item {
        ListItem(
            headlineContent = headline,
            supportingContent = supporting,
            trailingContent = trailing,
            leadingContent = leading,
            modifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier,
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent
            )
        )
    }
}
