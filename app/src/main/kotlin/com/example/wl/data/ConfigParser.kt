package com.example.wl.data

import android.util.Base64
import android.net.Uri
import java.nio.charset.StandardCharsets

object ConfigParser {
    fun parseSubscription(rawContent: String): List<ProxyConfig> {
        if (rawContent.startsWith("vless://") || rawContent.startsWith("vmess://") || 
            rawContent.startsWith("ss://") || rawContent.startsWith("trojan://")) {
            return listOfNotNull(parseLink(rawContent.trim()))
        }

        val decoded = try {
            val cleanContent = rawContent.replace("\n", "").replace("\r", "").trim()
            String(Base64.decode(cleanContent, Base64.DEFAULT), StandardCharsets.UTF_8)
        } catch (e: Exception) {
            rawContent
        }

        return decoded.lines()
            .filter { it.isNotBlank() }
            .mapNotNull { parseLink(it.trim()) }
    }

    private fun extractCountry(name: String): String {
        return "Other" // По требованию пользователя определяем только по IP
    }

    private fun parseLink(link: String): ProxyConfig? {
        return try {
            val uri = Uri.parse(link)
            val rawRemark = uri.fragment ?: "Unnamed Server"
            val remark = if (rawRemark.length > 256) rawRemark.take(256) + "..." else rawRemark
            val country = extractCountry(remark)
            
            when (uri.scheme) {
                "vless" -> {
                    val host = uri.host ?: ""
                    val port = uri.port
                    ProxyConfig(remark, "vless", host, port, link, country = country)
                }
                "vmess" -> {
                    try {
                        val vmessData = link.removePrefix("vmess://")
                        val decoded = Base64.decode(vmessData, Base64.DEFAULT)
                        val rawJson = String(decoded, StandardCharsets.UTF_8)
                        val json = org.json.JSONObject(rawJson)
                        val host = json.optString("add")
                        val port = json.optInt("port")
                        val rawName = json.optString("ps", remark)
                        val name = if (rawName.length > 256) rawName.take(256) + "..." else rawName
                        ProxyConfig(name, "vmess", host, port, link, country = extractCountry(name))
                    } catch (e: Exception) {
                        null
                    }
                }
                "ss" -> {
                    ProxyConfig(remark, "shadowsocks", uri.host ?: "", uri.port, link, country = country)
                }
                "trojan" -> {
                    ProxyConfig(remark, "trojan", uri.host ?: "", uri.port, link, country = country)
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
}
