package com.example.wl.data

import kotlinx.serialization.Serializable

@Serializable
data class ProxyConfig(
    val name: String,
    val type: String,
    val address: String,
    val port: Int,
    val fullUrl: String,
    var latency: Long? = null,
    val country: String = "Other"
)
