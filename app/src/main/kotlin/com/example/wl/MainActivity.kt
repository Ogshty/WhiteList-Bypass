package com.example.wl

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.work.*
import com.example.wl.data.SubscriptionWorker
import com.example.wl.ui.components.*
import com.example.wl.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private val viewModel: SubscriptionViewModel by viewModels()

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.onPermissionResult(result.resultCode == RESULT_OK, this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupBackgroundWork()
        setContent {
            WlTheme(animateTheme = true) {
                val context = LocalContext.current
                var currentScreen by remember { mutableStateOf<Screen>(Screen.Main) }

                LaunchedEffect(Unit) {
                    viewModel.vpnPermissionRequired.collect { intent ->
                        intent?.let { vpnPermissionLauncher.launch(it) }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
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
    val context = LocalContext.current
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
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
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
            AnimatedModeSelector(
                currentMode = connectionMode,
                onModeSelected = { viewModel.setConnectionMode(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

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
                        shape = MaterialTheme.shapes.large
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
                        TextButton(onClick = { viewModel.selectBestProxy(context) }) {
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
// SETTINGS SCREEN
// ============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SubscriptionViewModel, onBack: () -> Unit, onOpenLogs: () -> Unit) {
    val autoReconnect by viewModel.autoReconnect.collectAsState()
    val killSwitch by viewModel.killSwitch.collectAsState()
    val connectionMode by viewModel.connectionMode.collectAsState()
    val context = LocalContext.current
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
                },
                colors = TopAppBarDefaults.mediumTopAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                )
            )
        },
        containerColor = Color.Transparent
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
                                this.alpha = alpha
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
                .filter { 
                    val info = it.applicationInfo
                    info != null && (info.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM == 0)
                }
                .sortedBy { pm.getApplicationLabel(it.applicationInfo!!).toString() }
        }
    }

    val filteredApps = remember(apps, searchQuery) {
        apps.filter {
            val label = it.applicationInfo?.let { info -> pm.getApplicationLabel(info).toString() } ?: ""
            label.contains(searchQuery, ignoreCase = true) || it.packageName.contains(searchQuery, ignoreCase = true)
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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                )
            )
        },
        containerColor = Color.Transparent
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
                                app.applicationInfo?.let { info -> pm.getApplicationLabel(info).toString() } ?: app.packageName,
                                fontWeight = if (isChecked) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        supportingContent = { Text(app.packageName) },
                        leadingContent = {
                            val icon = remember(app.packageName) {
                                app.applicationInfo?.let { info -> 
                                    pm.getApplicationIcon(info).toBitmap().asImageBitmap()
                                }
                            }
                            if (icon != null) {
                                Image(
                                    bitmap = icon,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(MaterialTheme.shapes.extraLarge)
                                )
                            } else {
                                Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.extraLarge))
                            }
                        },
                        trailingContent = {
                            AnimatedSwitch(
                                checked = isChecked,
                                onCheckedChange = { viewModel.togglePackageWhitelist(app.packageName) }
                            )
                        },
                        modifier = Modifier
                            .clickable { viewModel.togglePackageWhitelist(app.packageName) }
                            .graphicsLayer { this.alpha = alpha }
                    )
                }
            }
        }
    }
}
