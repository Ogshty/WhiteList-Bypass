package com.example.wl.vpn

import com.example.wl.data.ProxyConfig
import org.json.JSONArray
import org.json.JSONObject

/**
 * Generates sing-box compatible JSON configuration.
 */
object SingBoxConfig {
    fun generate(
        proxy: ProxyConfig,
        whitelist: Set<String>,
        mode: String,
        killSwitch: Boolean,
        torBridgeType: String = "none",
        dpiSize: String = "1-500",
        dpiSleep: String = "0-500",
        dpiPacketType: String = "tlshello",
        dpiHttpFragment: Boolean = false,
        torDataDir: String = "tor-data"
    ): String {
        android.util.Log.d("SingBoxConfig", "Generating config start: mode=$mode, proxy=${proxy.name}")
        val config = JSONObject()

        // Log
        config.put("log", JSONObject().apply {
            put("level", "warn")
        })

        // DNS
        config.put("dns", JSONObject().apply {
            val servers = JSONArray()
            servers.put(JSONObject().apply {
                put("tag", "dns-remote")
                put("address", "https://1.1.1.1/dns-query")
            })
            servers.put(JSONObject().apply {
                put("tag", "dns-direct")
                put("address", "8.8.8.8")
                put("detour", "direct")
            })
            put("servers", servers)
            
            val rules = JSONArray()
            rules.put(JSONObject().apply {
                put("outbound", "direct")
                put("server", "dns-direct")
            })
            put("rules", rules)
            put("strategy", "prefer_ipv4")
        })

        // Inbounds: TUN
        val inbounds = JSONArray()
        inbounds.put(JSONObject().apply {
            put("type", "tun")
            put("tag", "tun-in")
            put("address", JSONArray().apply { put("172.19.0.1/24") })
            put("auto_route", false)
            put("strict_route", false)
            put("stack", "gvisor")
            put("mtu", 1500)
        })
        config.put("inbounds", inbounds)

        // Outbounds
        val outbounds = JSONArray()
        
        when (mode) {
            "tor" -> {
                outbounds.put(JSONObject().apply {
                    put("type", "tor")
                    put("tag", "tor-out")
                    put("data_directory", torDataDir)
                    
                    if (torBridgeType != "none") {
                        put("bridge", JSONObject().apply {
                            put("type", torBridgeType)
                            // Snowflake and Obfs4 might need more parameters, but sing-box
                            // often has built-in defaults or simple toggle.
                            // For a more advanced Orbot-like experience, we'd add more fields.
                        })
                    }
                })
            }
            "dpi" -> {
                outbounds.put(JSONObject().apply {
                    put("type", "fragment")
                    put("tag", "fragment-out")
                    put("method", dpiPacketType)
                    put("length", dpiSize)
                    put("interval", dpiSleep)
                    put("detour", "direct")
                })
            }
            else -> {
                // Default/Whitelist - Proxy Outbound
                outbounds.put(createOutbound(proxy))
            }
        }

        // Always include direct for bypass
        outbounds.put(JSONObject().apply {
            put("type", "direct")
            put("tag", "direct")
        })

        // Block Outbound
        outbounds.put(JSONObject().apply {
            put("type", "block")
            put("tag", "block")
        })

        config.put("outbounds", outbounds)

        // Routing
        config.put("route", JSONObject().apply {
            val rules = JSONArray()

            // Sniffing rule (migrated from inbound field in sing-box 1.11.0+)
            rules.put(JSONObject().apply {
                put("inbound", "tun-in")
                put("action", "sniff")
            })
            
            // 1. DNS rule
            rules.put(JSONObject().apply {
                put("protocol", "dns")
                put("outbound", "dns-remote")
            })

            // Bypass private IPs first
            rules.put(JSONObject().apply {
                put("ip_cidr", JSONArray().apply { 
                    put("10.0.0.0/8")
                    put("172.16.0.0/12")
                    put("192.168.0.0/16")
                    put("127.0.0.0/8")
                    put("169.254.0.0/16")
                })
                put("outbound", "direct")
            })

            // Bypass VPN itself to avoid loops
            rules.put(JSONObject().apply {
                put("package_name", JSONArray().apply { put("com.example.wl") })
                put("outbound", "direct")
            })

            // Fix for IP leakage: move mode handling inside specific rules
            // but ensure 'final' is only set once if needed.
            // Actually, in sing-box, first match wins.
            
            // Handle different modes
            when (mode) {
                "whitelist" -> {
                    if (whitelist.isNotEmpty()) {
                        rules.put(JSONObject().apply {
                            put("package_name", JSONArray().apply {
                                whitelist.forEach { put(it) }
                            })
                            put("outbound", "proxy")
                        })
                        // If something matches the package names, it goes to proxy.
                        // If not, it falls through.
                        if (killSwitch) {
                            rules.put(JSONObject().apply {
                                put("outbound", "block")
                            })
                        } else {
                            rules.put(JSONObject().apply {
                                put("outbound", "direct")
                            })
                        }
                    } else {
                        // Global proxy if whitelist is empty? 
                        // Or just let it fall through to 'final' below
                    }
                }
                "tor" -> {
                     rules.put(JSONObject().apply {
                        put("outbound", "tor-out")
                    })
                }
                "dpi" -> {
                     rules.put(JSONObject().apply {
                        put("outbound", "fragment-out")
                    })
                }
                else -> {
                     rules.put(JSONObject().apply {
                        put("outbound", "proxy")
                    })
                }
            }

            put("rules", rules)
            // put("final", "...") is better used when you don't have a catch-all rule
            // but for clarity we added catch-all rules above.
            put("auto_detect_interface", true)
        })

        val result = config.toString(4)
        android.util.Log.d("SingBoxConfig", "Generating config end")
        return result
    }

    private fun createOutbound(proxy: ProxyConfig): JSONObject {
        val outbound = JSONObject()
        outbound.put("tag", "proxy")
        
        val uri = android.net.Uri.parse(proxy.fullUrl)
        val scheme = uri.scheme?.lowercase() ?: ""
        
        when (scheme) {
            "vless" -> {
                outbound.put("type", "vless")
                outbound.put("server", proxy.address)
                outbound.put("server_port", proxy.port)
                outbound.put("uuid", uri.userInfo)
                
                val flow = uri.getQueryParameter("flow")
                if (flow != null && flow.isNotEmpty()) {
                    outbound.put("flow", flow)
                }

                val tls = JSONObject()
                val security = uri.getQueryParameter("security")
                if (security == "tls" || security == "reality") {
                    tls.put("enabled", true)
                    tls.put("server_name", uri.getQueryParameter("sni") ?: proxy.address)
                    
                    val utls = JSONObject()
                    utls.put("enabled", true)
                    utls.put("fingerprint", uri.getQueryParameter("fp") ?: "chrome")
                    tls.put("utls", utls)

                    if (security == "reality") {
                        tls.put("reality", JSONObject().apply {
                            put("enabled", true)
                            put("public_key", uri.getQueryParameter("pbk"))
                            put("short_id", uri.getQueryParameter("sid") ?: "")
                        })
                        // Reality doesn't use utls.fingerprint in some versions, it's inside reality
                        // but sing-box usually wants it in utls.
                    }
                }
                outbound.put("tls", tls)

                val transport = uri.getQueryParameter("type")
                if (transport != null && transport != "tcp") {
                    // xhttp is not supported in older libbox, skip it to avoid crash
                    if (transport != "xhttp") {
                        val transportObj = JSONObject()
                        transportObj.put("type", transport)
                        if (transport == "grpc") {
                            transportObj.put("service_name", uri.getQueryParameter("serviceName") ?: "")
                        } else if (transport == "ws") {
                             transportObj.put("path", uri.getQueryParameter("path") ?: "/")
                             val headers = JSONObject()
                             uri.getQueryParameter("host")?.let { headers.put("Host", it) }
                             transportObj.put("headers", headers)
                        }
                        outbound.put("transport", transportObj)
                    }
                }
            }
            "vmess" -> {
                outbound.put("type", "vmess")
                outbound.put("server", proxy.address)
                outbound.put("server_port", proxy.port)
                outbound.put("uuid", uri.userInfo)
                outbound.put("security", "auto")
            }
            "shadowsocks", "ss" -> {
                outbound.put("type", "shadowsocks")
                outbound.put("server", proxy.address)
                outbound.put("server_port", proxy.port)
                val userInfo = uri.userInfo ?: ""
                if (userInfo.contains(":")) {
                    val parts = userInfo.split(":")
                    outbound.put("method", parts[0])
                    outbound.put("password", parts[1])
                }
            }
            "trojan" -> {
                outbound.put("type", "trojan")
                outbound.put("server", proxy.address)
                outbound.put("server_port", proxy.port)
                outbound.put("password", uri.userInfo)
            }
            else -> {
                outbound.put("type", "direct")
            }
        }
        
        return outbound
    }

    private fun parseQuery(query: String?): Map<String, String> {
        val params = mutableMapOf<String, String>()
        query?.split("&")?.forEach { pair ->
            val parts = pair.split("=", limit = 2)
            if (parts.size == 2) {
                try {
                    params[parts[0]] = java.net.URLDecoder.decode(parts[1], "UTF-8")
                } catch (e: Exception) {
                    android.util.Log.e("SingBoxConfig", "Failed to decode param: ${parts[1]}", e)
                }
            }
        }
        return params
    }
}
