package com.example.wl

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.*
import com.example.wl.data.ProxyConfig
import com.example.wl.data.SubscriptionWorker
import com.example.wl.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class MainActivity : ComponentActivity() {
    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val viewModel: SubscriptionViewModel by viewModels()
        viewModel.onPermissionResult(result.resultCode == RESULT_OK, this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupBackgroundWork()
        setContent {
            WlTheme(animateTheme = true) {
                val viewModel: SubscriptionViewModel = viewModel()
                val context = LocalContext.current

                var currentScreen by remember { mutableStateOf<Screen>(Screen.Main) }

                LaunchedEffect(Unit) {
                    viewModel.vpnPermissionRequired.collect { intent ->
                        intent?.let { vpnPermissionLauncher.launch(it) }
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
                            (slideInHorizontally { direction * it } + fadeIn(tween(400))).togetherWith(
                                slideOutHorizontally { -direction * it } + fadeOut(tween(300))
                            ).using(SizeTransform(clip = false))
                        },
                        label = "ScreenTransition"
                    ) { screen ->
                        when (screen) {
                            is Screen.Main -> MainScreen(
                                viewModel = viewModel,
                                onConnectClick = { viewModel.toggleConnection(context) },
                                onOpenWhitelist = { currentScreen = Screen.Whitelist },
                                onOpenSettings = { currentScreen = Screen.Settings }
                            )
                            is Screen.Whitelist -> WhitelistScreen(
                                viewModel = viewModel,
                                onBack = { currentScreen = Screen.Main }
                            )
                            is Screen.Settings -> SettingsScreen(
                                viewModel = viewModel,
                                onBack = { currentScreen = Screen.Main },
                                onOpenLogs = { currentScreen = Screen.Logs }
                            )
                            is Screen.Logs -> LogScreen(
                                onBack = { currentScreen = Screen.Settings }
                            )
                        }
                    }
                }
            }
        }
    }

    sealed class Screen(val ordinal: Int = 0) {
        object Main : Screen(0)
        object Whitelist : Screen(1)
        object Settings : Screen(2)
        object Logs : Screen(3)
    }

    private fun setupBackgroundWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<SubscriptionWorker>(12, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "SubscriptionUpdate",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}

// ============================================================================
// Animated Gradient Background
// ============================================================================
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

// ============================================================================
// MAIN SCREEN
// ============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: SubscriptionViewModel,
    onConnectClick: () -> Unit,
    onOpenWhitelist: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val connectionState = viewModel.connectionState
    val isConnected = connectionState == SubscriptionViewModel.ConnectionState.CONNECTED
    val isConnecting = connectionState == SubscriptionViewModel.ConnectionState.CONNECTING
    val selectedProxy = viewModel.selectedProxy
    val connectionMode by viewModel.connectionMode.collectAsState()
    val listState = rememberLazyListState()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = Color.Transparent,
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.app_name),
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    if (connectionMode == "whitelist") {
                        IconButton(onClick = { viewModel.refreshSubscriptions() }) {
                            if (viewModel.isFetching) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                            }
                        }
                        IconButton(onClick = { viewModel.pingAll() }, enabled = !viewModel.isPinging) {
                            if (viewModel.isPinging) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Speed, contentDescription = null)
                            }
                        }
                    }
                    IconButton(onClick = onOpenWhitelist) {
                        Icon(if (connectionMode == "whitelist") Icons.AutoMirrored.Filled.List else Icons.Default.Public, contentDescription = null)
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = null)
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                )
            )
        },
        floatingActionButton = {
            PulseFloatingActionButton(
                isConnected = isConnected,
                isConnecting = isConnecting,
                onClick = onConnectClick
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Animated mode selector
            AnimatedModeSelector(
                currentMode = connectionMode,
                onModeSelected = { viewModel.setConnectionMode(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Expressive Status Card with parallax
            AnimatedStatusCard(
                viewModel = viewModel,
                connectionState = connectionState,
                selectedProxy = selectedProxy,
                connectionMode = connectionMode
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Error message
            AnimatedVisibility(
                visible = viewModel.errorMessage != null,
                enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = viewModel.errorMessage ?: "",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            AnimatedVisibility(
                visible = connectionMode == "whitelist",
                enter = fadeIn(tween(400)) + expandVertically(expandFrom = Alignment.Top),
                exit = fadeOut(tween(200)) + shrinkVertically(shrinkTowards = Alignment.Top)
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Locations",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = { viewModel.selectBestProxy() }) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Auto Select")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    ExpressiveProxyList(
                        viewModel = viewModel,
                        listState = listState
                    )
                }
            }
        }
    }
}

// ============================================================================
// PULSE FAB
// ============================================================================
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

    val containerColor by animateColorAsState(
        targetValue = when {
            isConnecting -> MaterialTheme.colorScheme.tertiaryContainer
            isConnected -> MaterialTheme.colorScheme.errorContainer
            else -> MaterialTheme.colorScheme.primaryContainer
        },
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "fabColor"
    )

    val shape = RoundedCornerShape(28.dp)

    Box(
        modifier = Modifier
            .scale(if (isConnecting) pulseScale else 1f)
            .graphicsLayer {
                // Glow effect
                shadowElevation = if (isConnecting) 24f else 8f
                ambientShadowColor = MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha)
                spotShadowColor = MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha)
            }
    ) {
        ExtendedFloatingActionButton(
            onClick = onClick,
            containerColor = containerColor,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
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
                    val labelFloat by animateFloatAsState(
                        targetValue = when {
                            isConnecting -> 0f
                            isConnected -> 1f
                            else -> 2f
                        },
                        animationSpec = tween(300),
                        label = "fabLabel"
                    )
                    Text(
                        text = when (labelFloat.toInt()) {
                            0 -> stringResource(R.string.status_connecting)
                            1 -> stringResource(R.string.disconnect)
                            else -> stringResource(R.string.connect)
                        },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        )
    }
}

// ============================================================================
// ANIMATED MODE SELECTOR
// ============================================================================
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

    val density = LocalDensity.current
    val itemWidth = with(density) { 120.dp.toPx() }
    val offset = animProgress * itemWidth

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            // Sliding indicator
            Box(
                modifier = Modifier
                    .offset(x = with(density) { offset.toDp() })
                    .width(120.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                modes.forEachIndexed { index, mode ->
                    val isSelected = currentMode == mode
                    Text(
                        text = modeLabels[index],
                        modifier = Modifier
                            .width(120.dp)
                            .height(40.dp)
                            .clickable { onModeSelected(mode) }
                            .wrapContentSize(Alignment.Center),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
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

// ============================================================================
// ANIMATED STATUS CARD (with 3D parallax)
// ============================================================================
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

    // Animated colors
    val containerColor by animateColorAsState(
        targetValue = when {
            isConnecting -> MaterialTheme.colorScheme.tertiaryContainer
            isConnected -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "cardColor"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            isConnecting -> MaterialTheme.colorScheme.tertiary
            isConnected -> MaterialTheme.colorScheme.primary
            else -> Color.Transparent
        },
        animationSpec = tween(600),
        label = "borderColor"
    )

    // Gradient overlay
    val gradientAlpha by animateFloatAsState(
        targetValue = if (isConnected || isConnecting) 1f else 0f,
        animationSpec = tween(800),
        label = "gradientAlpha"
    )

    // Parallax offset from pointer input
    var parallaxOffset by remember { mutableStateOf(Offset.Zero) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .graphicsLayer {
                // 3D tilt effect
                rotationX = parallaxOffset.y * 0.03f
                rotationY = -parallaxOffset.x * 0.03f
                cameraDistance = 12f * density
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val size = size
                        val position = event.changes.firstOrNull()?.position ?: Offset.Zero
                        parallaxOffset = Offset(
                            (position.x / size.width - 0.5f) * 2f,
                            (position.y / size.height - 0.5f) * 2f
                        )
                    }
                }
            },
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = if (borderColor != Color.Transparent) BorderStroke(1.5.dp, borderColor) else null
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Gradient overlay
            if (gradientAlpha > 0.01f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f * gradientAlpha),
                                    Color.Transparent
                                )
                            ),
                            shape = RoundedCornerShape(32.dp)
                        )
                )
            }

            // Glow dot in corner
            if (isConnected) {
                val glowTransition = rememberInfiniteTransition(label = "glow")
                val glowSize by glowTransition.animateFloat(
                    initialValue = 20f,
                    targetValue = 40f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "glowSize"
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .size(with(LocalDensity.current) { glowSize.toDp() })
                        .clip(CircleShape)
                        .background(StatusConnected.copy(alpha = 0.3f))
                )
            }

            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = when {
                        isConnecting -> stringResource(R.string.status_connecting)
                        isConnected -> stringResource(R.string.status_connected)
                        else -> stringResource(R.string.status_disconnected)
                    },
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isConnected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Tor progress
                AnimatedVisibility(
                    visible = isConnecting && connectionMode == "tor" && torProgress > 0,
                    enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                    exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
                ) {
                    Column {
                        LinearProgressIndicator(
                            progress = { torProgress / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .padding(vertical = 8.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            trackColor = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.15f)
                        )
                        Text(
                            text = "Bootstrapping: $torProgress%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                // Status row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Animated indicator dot
                    val dotColor by animateColorAsState(
                        targetValue = when {
                            isConnecting -> StatusConnecting
                            isConnected -> StatusConnected
                            else -> StatusDisconnected
                        },
                        animationSpec = tween(400),
                        label = "dotColor"
                    )

                    val dotPulse = rememberInfiniteTransition(label = "dotPulse")
                    val dotAlpha by dotPulse.animateFloat(
                        initialValue = 0.6f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(if (isConnecting) 600 else 1200, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dotAlpha"
                    )

                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(dotColor.copy(alpha = dotAlpha))
                            .graphicsLayer {
                                shadowElevation = 4f
                                ambientShadowColor = dotColor
                                spotShadowColor = dotColor
                            }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = when {
                                isConnecting -> stringResource(R.string.finding_server)
                                isConnected -> {
                                    when (connectionMode) {
                                        "tor" -> "Tor Network"
                                        "dpi" -> "Bye Bye DPI"
                                        else -> selectedProxy?.name ?: stringResource(R.string.traffic_routed)
                                    }
                                }
                                else -> stringResource(R.string.tap_to_connect)
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            maxLines = 1
                        )
                        AnimatedVisibility(visible = isConnected) {
                            val subText = when (connectionMode) {
                                "tor" -> "Anonymous routing active"
                                "dpi" -> "Anti-censorship active"
                                else -> if (selectedProxy != null) "${selectedProxy.type.uppercase()} • ${selectedProxy.address}" else null
                            }
                            if (subText != null) {
                                Text(
                                    text = subText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// COUNTRY SELECTOR (expressive)
// ============================================================================
@Composable
fun ExpressiveCountrySelector(viewModel: SubscriptionViewModel) {
    val countries = viewModel.countries
    val selectedCountry = viewModel.selectedCountry

    if (countries.isNotEmpty()) {
        Column {
            Text(
                text = "Select Country",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                item {
                    val isSelected = selectedCountry == null
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectCountry(null) },
                        label = { Text("All") },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                items(countries) { country ->
                    val isSelected = selectedCountry == country
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectCountry(country) },
                        label = {
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
                            Text("$flag $country", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }
    }
}

// ============================================================================
// EXPRESSIVE PROXY LIST
// ============================================================================
@Composable
fun ExpressiveProxyList(
    viewModel: SubscriptionViewModel,
    listState: androidx.compose.foundation.lazy.LazyListState
) {
    val proxyConfigs = viewModel.proxyConfigs
    val selectedCountry = viewModel.selectedCountry
    val isPinging = viewModel.isPinging
    val selectedProxy = viewModel.selectedProxy

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
                                    onClick = { viewModel.selectedProxy = proxy },
                                    index = index
                                )
                            }
                    }
                }
            }
        }
    }
}

// ============================================================================
// COUNTRY CARD (expressive)
// ============================================================================
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
        shape = RoundedCornerShape(24.dp),
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

// ============================================================================
// PROXY ITEM (expressive)
// ============================================================================
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

    // Staggered entrance
    // Staggered entrance — use LaunchedEffect for delay
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
                alpha = animatedFraction
                shadowElevation = elevation.value
            },
        shape = RoundedCornerShape(20.dp),
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

// ============================================================================
// SHIMMER LOADING SKELETON
// ============================================================================
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

// ============================================================================
// SETTINGS SCREEN
// ============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SubscriptionViewModel, onBack: () -> Unit, onOpenLogs: () -> Unit) {
    val autoReconnect by viewModel.autoReconnect.collectAsState()
    val killSwitch by viewModel.killSwitch.collectAsState()
    val connectionMode by viewModel.connectionMode.collectAsState()
    val torBridgeType by viewModel.torBridgeType.collectAsState()
    val dpiSize by viewModel.dpiFragmentSize.collectAsState()
    val dpiSleep by viewModel.dpiFragmentSleep.collectAsState()
    val dpiPacketType by viewModel.dpiPacketType.collectAsState()
    val dpiHttpFragment by viewModel.dpiHttpFragment.collectAsState()

    var showTorBridgeDialog by remember { mutableStateOf(false) }
    var showDpiSettingsDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            MediumTopAppBar(
                title = { Text(stringResource(R.string.settings), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.connection),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            settingsItem(
                headline = { Text(stringResource(R.string.auto_reconnect)) },
                supporting = { Text(stringResource(R.string.auto_reconnect_desc)) },
                trailing = {
                    AnimatedSwitch(
                        checked = autoReconnect,
                        onCheckedChange = { viewModel.toggleAutoReconnect(it) }
                    )
                }
            )
            settingsItem(
                headline = { Text(stringResource(R.string.kill_switch)) },
                supporting = { Text(stringResource(R.string.kill_switch_desc)) },
                trailing = {
                    AnimatedSwitch(
                        checked = killSwitch,
                        onCheckedChange = { viewModel.toggleKillSwitch(it) }
                    )
                }
            )
            settingsItem(
                headline = { Text(stringResource(R.string.logs)) },
                supporting = { Text("System VPN messages") },
                leading = { Icon(Icons.Default.Info, contentDescription = null) },
                onClick = onOpenLogs
            )

            if (connectionMode == "tor") {
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        text = "Tor Settings",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                settingsItem(
                    headline = { Text("Tor Bridges") },
                    supporting = {
                        Text(
                            when (torBridgeType) {
                                "snowflake" -> "Snowflake (Bypass censorship)"
                                "obfs4" -> "Obfs4 (Advanced obfuscation)"
                                else -> "None (Direct connection to Tor)"
                            }
                        )
                    },
                    leading = { Icon(Icons.Default.Security, contentDescription = null) },
                    onClick = { showTorBridgeDialog = true }
                )
            }

            if (connectionMode == "dpi") {
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        text = "Bye Bye DPI Settings",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                settingsItem(
                    headline = { Text("Fragmentation Settings") },
                    supporting = { Text("Size: $dpiSize, Sleep: $dpiSleep") },
                    leading = { Icon(Icons.Default.Tune, contentDescription = null) },
                    onClick = { showDpiSettingsDialog = true }
                )
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    if (showTorBridgeDialog) {
        TorBridgeDialog(
            currentType = torBridgeType,
            onDismiss = { showTorBridgeDialog = false },
            onSelect = { type ->
                viewModel.setTorBridgeType(type)
                showTorBridgeDialog = false
            }
        )
    }

    if (showDpiSettingsDialog) {
        DpiSettingsDialog(
            currentSize = dpiSize,
            currentSleep = dpiSleep,
            currentPacketType = dpiPacketType,
            currentHttpFragment = dpiHttpFragment,
            onDismiss = { showDpiSettingsDialog = false },
            onConfirm = { size, sleep, type, http ->
                viewModel.setDpiFragmentSize(size)
                viewModel.setDpiFragmentSleep(sleep)
                viewModel.setDpiPacketType(type)
                viewModel.setDpiHttpFragment(http)
                showDpiSettingsDialog = false
            },
            onReset = { viewModel.resetDpiSettings() }
        )
    }
}

// Helper extension on LazyListScope for settings items
private fun LazyListScope.settingsItem(
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

// ============================================================================
// ANIMATED SWITCH
// ============================================================================
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

// ============================================================================
// DPI SETTINGS DIALOG
// ============================================================================
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
        title = { Text("DPI Fragmentation Settings", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = size,
                    onValueChange = { size = it },
                    label = { Text("Size Range (e.g. 1-500)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = sleep,
                    onValueChange = { sleep = it },
                    label = { Text("Sleep Range (e.g. 0-500)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
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

// ============================================================================
// TOR BRIDGE DIALOG
// ============================================================================
@Composable
fun TorBridgeDialog(currentType: String, onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    val options = listOf(
        "none" to "None (Direct)",
        "snowflake" to "Snowflake (Anti-censorship)",
        "obfs4" to "Obfs4 (Obfuscated)"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Tor Bridge", fontWeight = FontWeight.Bold) },
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

// ============================================================================
// LOG SCREEN (terminal-style)
// ============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(onBack: () -> Unit) {
    val logs by com.example.wl.vpn.VpnLogManager.logs.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.logs), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { com.example.wl.vpn.VpnLogManager.clear() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceContainerLowest,
                            MaterialTheme.colorScheme.surfaceContainer
                        )
                    )
                )
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp)
            ) {
                items(logs) { log ->
                    val isNew = remember { true }
                    val alpha by animateFloatAsState(
                        targetValue = 1f,
                        animationSpec = tween(500),
                        label = "logEntry"
                    )

                    Text(
                        text = log,
                        style = TextStyle(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .graphicsLayer {
                                alpha = alpha
                            }
                    )
                }
            }
        }
    }
}

// ============================================================================
// WHITELIST SCREEN
// ============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhitelistScreen(viewModel: SubscriptionViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val pm = context.packageManager
    val whitelist by viewModel.whitelistPackages.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    val apps by produceState<List<android.content.pm.PackageInfo>>(initialValue = emptyList()) {
        value = withContext(Dispatchers.IO) {
            pm.getInstalledPackages(0)
                .filter { it.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM == 0 }
                .sortedBy { pm.getApplicationLabel(it.applicationInfo).toString() }
        }
    }

    val filteredApps = remember(apps, searchQuery) {
        apps.filter {
            pm.getApplicationLabel(it.applicationInfo).toString().contains(searchQuery, ignoreCase = true) ||
                    it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.whitelist_apps), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            // Animated search bar
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onSearch = { },
                active = isSearchActive,
                onActiveChange = { isSearchActive = it },
                placeholder = { Text(stringResource(R.string.search_apps)) },
                leadingIcon = {
                    Icon(
                        if (isSearchActive) Icons.Default.ArrowBack else Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.clickable { isSearchActive = !isSearchActive }
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Clear",
                            modifier = Modifier.clickable { searchQuery = "" }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .animateContentSize(),
                colors = SearchBarDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) { }

            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = filteredApps,
                    key = { it.packageName }
                ) { app ->
                    val isChecked = whitelist.contains(app.packageName)

                    val alpha by animateFloatAsState(
                        targetValue = 1f,
                        animationSpec = tween(400),
                        label = "appAlpha"
                    )

                    ListItem(
                        headlineContent = {
                            Text(
                                pm.getApplicationLabel(app.applicationInfo).toString(),
                                fontWeight = if (isChecked) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        supportingContent = { Text(app.packageName) },
                        leadingContent = {
                            val icon = remember(app.packageName) {
                                pm.getApplicationIcon(app.applicationInfo).toBitmap().asImageBitmap()
                            }
                            Image(
                                bitmap = icon,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        },
                        trailingContent = {
                            AnimatedSwitch(
                                checked = isChecked,
                                onCheckedChange = { viewModel.togglePackageWhitelist(app.packageName) }
                            )
                        },
                        modifier = Modifier
                            .clickable { viewModel.togglePackageWhitelist(app.packageName) }
                            .graphicsLayer { alpha = alpha }
                    )
                }
            }
        }
    }
}