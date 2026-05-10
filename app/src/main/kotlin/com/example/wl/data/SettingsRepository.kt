package com.example.wl.data

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private val SELECTED_PROXY_URL = stringPreferencesKey("selected_proxy_url")
    private val CACHED_PROXIES = stringPreferencesKey("cached_proxies")
    private val WHITELIST_PACKAGES = stringPreferencesKey("whitelist_packages")
    private val SUBSCRIPTIONS = stringPreferencesKey("subscriptions")
    private val AUTO_RECONNECT = booleanPreferencesKey("auto_reconnect")
    private val KILL_SWITCH = booleanPreferencesKey("kill_switch")
    private val CONNECTION_MODE = stringPreferencesKey("connection_mode")
    private val TOR_BRIDGE_TYPE = stringPreferencesKey("tor_bridge_type")
    private val DPI_FRAGMENT_SIZE = stringPreferencesKey("dpi_fragment_size")
    private val DPI_FRAGMENT_SLEEP = stringPreferencesKey("dpi_fragment_sleep")
    private val DPI_PACKET_TYPE = stringPreferencesKey("dpi_packet_type")
    private val DPI_HTTP_FRAGMENT = booleanPreferencesKey("dpi_http_fragment")
    private val GEOIP_CACHE = stringPreferencesKey("geoip_cache")

    val geoipCache: Flow<Map<String, String>> = context.dataStore.data
        .map { preferences ->
            val json = preferences[GEOIP_CACHE] ?: return@map emptyMap<String, String>()
            try {
                Json.decodeFromString<Map<String, String>>(json)
            } catch (e: Exception) {
                emptyMap()
            }
        }

    suspend fun updateGeoipCache(ip: String, country: String) {
        context.dataStore.edit { preferences ->
            val json = preferences[GEOIP_CACHE]
            val current = try {
                if (json != null) Json.decodeFromString<Map<String, String>>(json).toMutableMap()
                else mutableMapOf()
            } catch (e: Exception) {
                mutableMapOf<String, String>()
            }
            current[ip] = country
            preferences[GEOIP_CACHE] = Json.encodeToString(current)
        }
    }

    val connectionMode: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[CONNECTION_MODE] ?: "whitelist"
        }

    suspend fun setConnectionMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[CONNECTION_MODE] = mode
        }
    }

    val torBridgeType: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[TOR_BRIDGE_TYPE] ?: "none"
        }

    suspend fun setTorBridgeType(type: String) {
        context.dataStore.edit { preferences ->
            preferences[TOR_BRIDGE_TYPE] = type
        }
    }

    val dpiFragmentSize: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[DPI_FRAGMENT_SIZE] ?: "1-500"
        }

    suspend fun setDpiFragmentSize(size: String) {
        context.dataStore.edit { preferences ->
            preferences[DPI_FRAGMENT_SIZE] = size
        }
    }

    val dpiFragmentSleep: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[DPI_FRAGMENT_SLEEP] ?: "0-500"
        }

    suspend fun setDpiFragmentSleep(sleep: String) {
        context.dataStore.edit { preferences ->
            preferences[DPI_FRAGMENT_SLEEP] = sleep
        }
    }

    val dpiPacketType: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[DPI_PACKET_TYPE] ?: "tlshello"
        }

    suspend fun setDpiPacketType(type: String) {
        context.dataStore.edit { preferences ->
            preferences[DPI_PACKET_TYPE] = type
        }
    }

    val dpiHttpFragment: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[DPI_HTTP_FRAGMENT] ?: false
        }

    suspend fun setDpiHttpFragment(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DPI_HTTP_FRAGMENT] = enabled
        }
    }

    val selectedProxyUrl: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[SELECTED_PROXY_URL]
        }

    val cachedProxies: Flow<List<ProxyConfig>> = context.dataStore.data
        .map { preferences ->
            val json = preferences[CACHED_PROXIES]
            if (json == null) {
                Log.d("SettingsRepository", "Cache is empty")
                return@map emptyList<ProxyConfig>()
            }
            try {
                val proxies = Json.decodeFromString<List<ProxyConfig>>(json)
                Log.d("SettingsRepository", "Loaded ${proxies.size} proxies from cache")
                proxies
            } catch (e: Exception) {
                Log.e("SettingsRepository", "Error decoding cache", e)
                emptyList()
            }
        }

    suspend fun setSelectedProxyUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[SELECTED_PROXY_URL] = url
        }
    }

    suspend fun setCachedProxies(proxies: List<ProxyConfig>) {
        try {
            val json = Json.encodeToString(proxies)
            context.dataStore.edit { preferences ->
                preferences[CACHED_PROXIES] = json
            }
            Log.d("SettingsRepository", "Saved ${proxies.size} proxies to cache")
        } catch (e: Exception) {
            Log.e("SettingsRepository", "Error saving cache", e)
        }
    }

    val whitelistPackages: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            preferences[WHITELIST_PACKAGES]?.split(",")?.filter { it.isNotEmpty() }?.toSet() ?: emptySet()
        }

    suspend fun setWhitelistPackages(packages: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[WHITELIST_PACKAGES] = packages.joinToString(",")
        }
    }

    val autoReconnect: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[AUTO_RECONNECT] ?: true
        }

    suspend fun setAutoReconnect(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_RECONNECT] = enabled
        }
    }

    val killSwitch: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[KILL_SWITCH] ?: false
        }

    suspend fun setKillSwitch(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KILL_SWITCH] = enabled
        }
    }

    val savedSubscriptions: Flow<List<Subscription>> = context.dataStore.data
        .map { preferences ->
            val json = preferences[SUBSCRIPTIONS]
            if (json == null) return@map emptyList<Subscription>()
            try {
                Json.decodeFromString<List<Subscription>>(json)
            } catch (e: Exception) {
                emptyList()
            }
        }

    suspend fun setSubscriptions(subscriptions: List<Subscription>) {
        val json = Json.encodeToString(subscriptions)
        context.dataStore.edit { preferences ->
            preferences[SUBSCRIPTIONS] = json
        }
    }
}
