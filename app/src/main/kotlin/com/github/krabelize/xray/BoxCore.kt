package com.github.krabelize.xray

import android.util.Log
import io.nekohasekai.libbox.*
import java.io.File

object BoxCore {
    private const val TAG = "BoxCore"
    private var commandServer: CommandServer? = null
    var tunInterfaceProvider: TunInterfaceProvider? = null
    private var isInitialized = false
    private val lock = Any()
    private var stopInProgress = false

    interface TunInterfaceProvider {
        fun openTun(options: TunOptions?): Int
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
                    commandServer = Libbox.newCommandServer(EmptyHandler(), BoxPlatform())
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
        override fun autoDetectInterfaceControl(fd: Int) {}
        override fun clearDNSCache() {}
        override fun closeDefaultInterfaceMonitor(listener: InterfaceUpdateListener?) {}
        override fun findConnectionOwner(ipProtocol: Int, sourceAddress: String?, sourcePort: Int, destinationAddress: String?, destinationPort: Int): ConnectionOwner? = null
        override fun getInterfaces(): NetworkInterfaceIterator? = null
        override fun includeAllNetworks(): Boolean = false
        override fun localDNSTransport(): LocalDNSTransport? = null
        override fun openTun(options: TunOptions?): Int {
            Log.d(TAG, "openTun called from sing-box")
            return tunInterfaceProvider?.openTun(options) ?: -1
        }
        override fun readWIFIState(): WIFIState? = null
        override fun sendNotification(notification: Notification?) {}
        override fun startDefaultInterfaceMonitor(listener: InterfaceUpdateListener?) {}
        override fun usePlatformAutoDetectInterfaceControl(): Boolean = false
        override fun underNetworkExtension(): Boolean = false
        override fun useProcFS(): Boolean = false
        override fun systemCertificates(): StringIterator? = null
    }
}
