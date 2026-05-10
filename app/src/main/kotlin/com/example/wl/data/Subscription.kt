package com.example.wl.data

import kotlinx.serialization.Serializable

@Serializable
data class Subscription(
    val name: String,
    val url: String,
    var isEnabled: Boolean = true
)
