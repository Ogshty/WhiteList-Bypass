package com.github.krabelize.xray

import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import android.util.Log
import io.nekohasekai.libbox.*
import java.io.File
import java.net.InetSocketAddress

object BoxCore {
    private const val TAG = "BoxCore"
    private var commandServer: CommandServer? = null
    var tunInterfaceProvider: TunInterfaceProvider? = null
    var context: Context? = null
    private var isInitialized = false
    private val lock = Any()
    private var stopInProgress = false
    
    private val handler = EmptyHandler()
    private val platform = BoxPlatform()

    interface TunInterfaceProvider {
        fun openTun(options: TunOptions?): Int
        fun protect(fd: Int): Boolean
    }

    /**
     * Инициализация базовых путей
     */
    fun init(basePath: String) {
        if (isInitialized) return
        try {
            val options = SetupOptions()
            options.basePath = basePath
            options.workingPath = basePath
            options.tempPath = File(basePath, "tmp").apply { mkdirs() }.absolutePath
            
            // Удаляем старый сокет если он есть
            File(basePath, "command.sock").delete()
            
            Libbox.setup(options)
            isInitialized = true
            Log.d(TAG, "sing-box setup completed")
        } catch (e: Exception) {
            Log.e(TAG, "Setup failed", e)
        }
    }

    /**
     * Запуск ядра
     */
    fun start(config: String) {
        synchronized(lock) {
            try {
                Log.d(TAG, "Starting CommandServer...")
                
                // Ждём завершения предыдущей остановки (closeService может быть асинхронным внутри натива)
                var retries = 0
                while (stopInProgress && retries < 20) {
                    Log.d(TAG, "Waiting for previous stop to complete...")
                    Thread.sleep(50)
                    retries++
                }
                
                if (commandServer == null) {
                    commandServer = Libbox.newCommandServer(handler, platform)
                    commandServer?.start()
                }

                Log.d(TAG, "Applying configuration...")
                commandServer?.startOrReloadService(config, OverrideOptions())
                Log.d(TAG, "sing-box is running")
            } catch (e: Exception) {
                Log.e(TAG, "Start failed", e)
            }
        }
    }

    fun stop() {
        synchronized(lock) {
            stopInProgress = true
            try {
                commandServer?.closeService()
                Log.d(TAG, "sing-box stopped")
            } catch (e: Exception) {
                Log.d(TAG, "Stop ignored or failed: ${e.message}")
            } finally {
                commandServer = null
                stopInProgress = false
            }
            return
        }
    }

    private class EmptyHandler : CommandServerHandler {
        override fun getSystemProxyStatus(): SystemProxyStatus? = null
        override fun serviceReload() {}
        override fun serviceStop() {}
        override fun setSystemProxyEnabled(enabled: Boolean) {}
        override fun writeDebugMessage(message: String?) {
            if (message != null) {
                Log.d("BoxCore-Debug", message)
                com.example.wl.vpn.VpnLogManager.addLog(message)
                
                // Parse Tor progress: [tor] Bootstrapped 85% (enough_dirinfo): Finishing handshake with directory server
                if (message.contains("[tor] Bootstrapped")) {
                    val regex = Regex("""(\d+)%""")
                    regex.find(message)?.let {
                        val progress = it.groupValues[1].toIntOrNull() ?: 0
                        com.example.wl.vpn.VpnConnectionState.updateTorProgress(progress)
                    }
                }
            }
        }
    }

    private class BoxPlatform : PlatformInterface {
        override fun autoDetectInterfaceControl(fd: Int) {
            tunInterfaceProvider?.protect(fd)
        }
        override fun clearDNSCache() {}
        override fun closeDefaultInterfaceMonitor(listener: InterfaceUpdateListener?) {}
        override fun findConnectionOwner(ipProtocol: Int, sourceAddress: String?, sourcePort: Int, destinationAddress: String?, destinationPort: Int): ConnectionOwner {
            val owner = ConnectionOwner()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && context != null) {
                try {
                    val cm = context!!.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                    val uid = cm.getConnectionOwnerUid(
                        ipProtocol,
                        InetSocketAddress(sourceAddress, sourcePort),
                        InetSocketAddress(destinationAddress, destinationPort)
                    )
                    if (uid != -1) {
                        owner.userId = uid
                    }
                } catch (e: Exception) {
                    // Log.e(TAG, "findConnectionOwner failed", e)
                }
            }
            return owner
        }
        override fun getInterfaces(): NetworkInterfaceIterator = NetworkInterfaceIteratorImpl()
        override fun includeAllNetworks(): Boolean = false
        override fun localDNSTransport(): LocalDNSTransport? = null
        override fun openTun(options: TunOptions?): Int {
            Log.d(TAG, "openTun called from sing-box")
            return tunInterfaceProvider?.openTun(options) ?: -1
        }
        override fun readWIFIState(): WIFIState? = null
        override fun sendNotification(notification: Notification?) {}
        override fun startDefaultInterfaceMonitor(listener: InterfaceUpdateListener?) {}
        override fun usePlatformAutoDetectInterfaceControl(): Boolean = true
        override fun underNetworkExtension(): Boolean = false
        override fun useProcFS(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
        override fun systemCertificates(): StringIterator = StringIteratorImpl(emptyList())
    }

    private class StringIteratorImpl(val list: List<String>) : StringIterator {
        private var index = 0
        override fun hasNext(): Boolean = index < list.size
        override fun len(): Int = list.size
        override fun next(): String? = if (hasNext()) list[index++] else null
    }

    private class NetworkInterfaceIteratorImpl : NetworkInterfaceIterator {
        private val interfaces = try {
            java.net.NetworkInterface.getNetworkInterfaces()?.toList() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        private var index = 0
        override fun hasNext(): Boolean = index < interfaces.size
        override fun next(): io.nekohasekai.libbox.NetworkInterface? {
            if (!hasNext()) return null
            val ni = interfaces[index++]
            return try {
                val boxNi = io.nekohasekai.libbox.NetworkInterface()
                boxNi.name = ni.name
                boxNi.index = ni.index
                boxNi.mtu = ni.mtu
                val addresses = ni.interfaceAddresses.mapNotNull { it.address.hostAddress }
                boxNi.addresses = StringIteratorImpl(addresses)
                boxNi
            } catch (e: Exception) {
                null
            }
        }
    }
}
