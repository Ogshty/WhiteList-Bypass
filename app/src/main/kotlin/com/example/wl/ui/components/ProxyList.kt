package com.example.wl.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.wl.R
import com.example.wl.SubscriptionViewModel
import com.example.wl.data.ProxyConfig
import com.example.wl.ui.theme.LatencyFast
import com.example.wl.ui.theme.LatencyMedium
import kotlinx.coroutines.delay

@Composable
fun ExpressiveProxyList(
    viewModel: SubscriptionViewModel,
    listState: LazyListState
) {
    val proxyConfigs = viewModel.proxyConfigs
    val selectedCountry = viewModel.selectedCountry
    val isPinging = viewModel.isPinging
    val selectedProxy = viewModel.selectedProxy
    val context = androidx.compose.ui.platform.LocalContext.current

    val expandedStates = remember { mutableStateMapOf<String, Boolean>() }
    val groupedProxies = remember(proxyConfigs) {
        proxyConfigs.groupBy { it.country }
    }
    val countries = remember(groupedProxies) {
        groupedProxies.keys.toList().sorted()
    }

    if (countries.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (viewModel.isFetching) {
                ShimmerLoadingSkeleton()
            } else {
                Text(
                    stringResource(R.string.no_servers),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            items(
                items = countries,
                key = { it }
            ) { country ->
                val proxies = groupedProxies[country] ?: emptyList()
                val isSelected = selectedCountry == country
                val isExpanded = expandedStates[country] ?: false

                CountryExpressiveCard(
                    country = country,
                    count = proxies.size,
                    isSelected = isSelected,
                    isExpanded = isExpanded,
                    onExpandClick = {
                        expandedStates[country] = !isExpanded
                    },
                    onSelectClick = {
                        viewModel.selectCountry(country)
                    },
                    modifier = Modifier.animateItem()
                )

                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        proxies
                            .sortedBy { it.latency ?: Long.MAX_VALUE }
                            .forEachIndexed { index, proxy ->
                                ExpressiveProxyItem(
                                    proxy = proxy,
                                    isSelected = selectedProxy?.fullUrl == proxy.fullUrl,
                                    isPinging = isPinging,
                                    onClick = { viewModel.selectProxy(proxy, context) },
                                    index = index
                                )
                            }
                    }
                }
            }
        }
    }
}

@Composable
fun CountryExpressiveCard(
    country: String,
    count: Int,
    isSelected: Boolean,
    isExpanded: Boolean,
    onExpandClick: () -> Unit,
    onSelectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val flag = when (country) {
        "Russia" -> "🇷🇺"
        "Germany" -> "🇩🇪"
        "USA" -> "🇺🇸"
        "Netherlands" -> "🇳🇱"
        "Finland" -> "🇫🇮"
        "Turkey" -> "🇹🇷"
        "France" -> "🇫🇷"
        "Kazakhstan" -> "🇰🇿"
        "Ukraine" -> "🇺🇦"
        "Poland" -> "🇵🇱"
        else -> "🏳️"
    }

    val containerColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = tween(400),
        label = "cardColor"
    )

    val scale by animateFloatAsState(
        if (isSelected) 1.02f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "cardScale"
    )

    Surface(
        onClick = onSelectClick,
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = MaterialTheme.shapes.extraLarge,
        color = containerColor,
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = flag,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.headlineSmall
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = country,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$count servers available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onExpandClick) {
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(
                visible = isSelected,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
fun ExpressiveProxyItem(
    proxy: ProxyConfig,
    isSelected: Boolean,
    isPinging: Boolean,
    onClick: () -> Unit,
    index: Int = 0
) {
    val containerColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = tween(400),
        label = "itemColor"
    )

    val scale by animateFloatAsState(if (isSelected) 1.02f else 1f, label = "itemScale")
    val elevation by animateDpAsState(if (isSelected) 4.dp else 0.dp, label = "itemElevation")

    var startAnimation by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay((index * 50).toLong())
        startAnimation = true
    }
    val animatedFraction by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "entrance"
    )

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale * animatedFraction.coerceIn(0.5f, 1f)
                scaleY = scale * animatedFraction.coerceIn(0.5f, 1f)
                this.alpha = animatedFraction
                shadowElevation = elevation.value
            },
        shape = MaterialTheme.shapes.large,
        color = containerColor,
        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        ListItem(
            headlineContent = {
                Text(
                    proxy.name,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            supportingContent = {
                Text(
                    "${proxy.type.uppercase()} • ${proxy.address}",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            leadingContent = {
                val icon = when (proxy.type.lowercase()) {
                    "vless" -> Icons.Default.Security
                    "vmess" -> Icons.Default.Cloud
                    "ss" -> Icons.Default.Lock
                    else -> Icons.Default.Public
                }
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingContent = {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center
                ) {
                    Crossfade(targetState = isPinging, animationSpec = tween(300), label = "pingState") { pinging ->
                        if (pinging) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            proxy.latency?.let { latency ->
                                val latencyColor = when {
                                    latency < 0 -> MaterialTheme.colorScheme.error
                                    latency < 150 -> LatencyFast
                                    latency < 400 -> LatencyMedium
                                    else -> MaterialTheme.colorScheme.error
                                }
                                Surface(
                                    color = latencyColor.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = if (latency < 0) "Timeout" else "${latency}ms",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = latencyColor,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    RadioButton(
                        selected = isSelected,
                        onClick = onClick,
                        colors = RadioButtonDefaults.colors(
                            selectedColor = MaterialTheme.colorScheme.primary,
                            unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}

@Composable
fun ShimmerLoadingSkeleton() {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(5) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = alpha)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.3f)
                    ) {}
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(0.6f).height(16.dp),
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.3f)
                        ) {}
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(0.4f).height(12.dp),
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.2f)
                        ) {}
                    }
                }
            }
        }
    }
}
