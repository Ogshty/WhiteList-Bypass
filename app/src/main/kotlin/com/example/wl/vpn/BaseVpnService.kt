package com.example.wl.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.File
import java.net.InetAddress
import androidx.core.app.NotificationCompat
import com.example.wl.MainActivity
import com.example.wl.data.ProxyConfig
import com.example.wl.data.SettingsRepository
import com.github.krabelize.xray.BoxCore
import io.nekohasekai.libbox.TunOptions
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

abstract class BaseVpnService : VpnService(), BoxCore.TunInterfaceProvider {

    protected val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    protected lateinit var repository: SettingsRepository
    protected var lastProxy: ProxyConfig? = null
    private var isNetworkCallbackRegistered = false
    private var vpnInterface: ParcelFileDescriptor? = null

    companion object {
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "vpn_service"
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            serviceScope.launch {
                val autoReconnect = repository.autoReconnect.first()
                if (autoReconnect && lastProxy != null) {
                    startVpn(lastProxy!!)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        repository = SettingsRepository(this)
        createNotificationChannel()
        BoxCore.tunInterfaceProvider = this
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "VPN Status",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows VPN connection status"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    protected abstract fun getNotificationTitle(): String
    protected abstract fun getModeName(): String

    private fun createNotification(proxyName: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getNotificationTitle())
            .setContentText("Connected to $proxyName")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            serviceScope.launch {
                val autoReconnect = repository.autoReconnect.first()
                val lastProxyUrl = repository.selectedProxyUrl.first()
                if (autoReconnect) {
                    val mode = repository.connectionMode.first()
                    if (mode == getModeName()) {
                        if (mode == "whitelist" && lastProxyUrl != null) {
                            val cached = repository.cachedProxies.first()
                            cached.find { it.fullUrl == lastProxyUrl }?.let {
                                lastProxy = it
                                startVpn(it)
                                registerNetworkCallback()
                            }
                        } else if (mode != "whitelist") {
                            val dummyProxy = ProxyConfig(getNotificationTitle(), mode, "127.0.0.1", 0, "")
                            lastProxy = dummyProxy
                            startVpn(dummyProxy)
                            registerNetworkCallback()
                        }
                    }
                }
            }
            return START_STICKY
        }

        when (intent.action) {
            "START" -> {
                val proxy = ProxyConfig(
                    intent.getStringExtra("PROXY_NAME") ?: "",
                    intent.getStringExtra("PROXY_TYPE") ?: "",
                    intent.getStringExtra("PROXY_ADDRESS") ?: "",
                    intent.getIntExtra("PROXY_PORT", 443),
                    intent.getStringExtra("PROXY_URL") ?: ""
                )
                lastProxy = proxy
                startVpn(proxy)
                registerNetworkCallback()
            }
            "STOP" -> {
                unregisterNetworkCallback()
                stopVpn()
            }
        }
        return START_STICKY
    }

    private fun startVpn(proxy: ProxyConfig) {
        Log.d("BaseVpnService", "Starting VPN [${getModeName()}] for: ${proxy.name}")
        if (Build.VERSION.SDK_INT >= 34) { // UPSIDE_DOWN_CAKE
            startForeground(NOTIFICATION_ID, createNotification(proxy.name), 0x40000000) // FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else if (Build.VERSION.SDK_INT >= 29) { // Q
            startForeground(NOTIFICATION_ID, createNotification(proxy.name), 0x00000100) // FOREGROUND_SERVICE_TYPE_VPN
        } else {
            startForeground(NOTIFICATION_ID, createNotification(proxy.name))
        }
        serviceScope.launch(Dispatchers.IO) {
            try {
                VpnConnectionState.update(VpnConnectionState.Status.CONNECTING, getModeName())
                
                val packages = repository.whitelistPackages.first()
                val killSwitch = repository.killSwitch.first()
                val torBridge = repository.torBridgeType.first()
                val dpiSize = repository.dpiFragmentSize.first()
                val dpiSleep = repository.dpiFragmentSleep.first()
                val dpiPacketType = repository.dpiPacketType.first()
                val dpiHttpFragment = repository.dpiHttpFragment.first()
                
                val torDataDir = File(filesDir, "tor-data").apply { mkdirs() }.absolutePath
                val config = SingBoxConfig.generate(proxy, packages, getModeName(), killSwitch, torBridge, dpiSize, dpiSleep, dpiPacketType, dpiHttpFragment, torDataDir)
                Log.d("BaseVpnService", "Generated config: $config")
                
                VpnLogManager.addLog("[${getModeName()}] Starting core...")
                BoxCore.stop()
                BoxCore.init(filesDir.absolutePath)
                BoxCore.start(config)

                VpnLogManager.addLog("[${getModeName()}] Connected to ${proxy.name}")
                VpnConnectionState.update(VpnConnectionState.Status.CONNECTED, getModeName())
            } catch (e: Exception) {
                Log.e("BaseVpnService", "Failed to start VPN", e)
                VpnLogManager.addLog("Error: ${e.message}")
                withContext(Dispatchers.Main) { stopVpn() }
            }
        }
    }

    override fun openTun(options: TunOptions?): Int {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = Builder()
            .setSession(getNotificationTitle())
            .setMtu(options?.mtu ?: 1500)
            .addAddress("172.19.0.1", 24)
            .addRoute("0.0.0.0", 0)
            .addDnsServer("1.1.1.1")
            .addDnsServer("8.8.8.8")
            .setConfigureIntent(pendingIntent)
        
        try {
            builder.addDisallowedApplication(packageName)
        } catch (e: Exception) {
            Log.e("BaseVpnService", "Failed to exclude package", e)
        }

        vpnInterface = try {
            builder.establish()
        } catch (e: Exception) {
            Log.e("BaseVpnService", "establish() failed", e)
            null
        }
        Log.d("BaseVpnService", "VPN interface established: ${vpnInterface != null}")
        if (vpnInterface == null) {
            VpnLogManager.addLog("Error: VPN interface could not be established. Check VPN permissions.")
        }
        return vpnInterface?.detachFd() ?: -1
    }

    private fun registerNetworkCallback() {
        if (isNetworkCallbackRegistered) return
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        cm.registerDefaultNetworkCallback(networkCallback)
        isNetworkCallbackRegistered = true
    }

    private fun unregisterNetworkCallback() {
        if (!isNetworkCallbackRegistered) return
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        cm.unregisterNetworkCallback(networkCallback)
        isNetworkCallbackRegistered = false
    }

    private fun stopVpn() {
        Log.d("BaseVpnService", "Stopping VPN")
        BoxCore.stop()
        try {
            vpnInterface?.close()
        } catch (e: Exception) {
            Log.e("BaseVpnService", "Error closing vpnInterface", e)
        }
        vpnInterface = null
        VpnConnectionState.update(VpnConnectionState.Status.DISCONNECTED, null)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            stopForeground(true)
        }
        stopSelf()
    }

    override fun onRevoke() {
        stopVpn()
        super.onRevoke()
    }

    override fun onDestroy() {
        // Сначала останавливаем VPN и освобождаем native ресурсы, потом отменяем корутины
        stopVpn()
        serviceScope.cancel()
        super.onDestroy()
    }
}
