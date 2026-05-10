package com.example.wl.vpn

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object VpnConnectionState {
    enum class Status {
        DISCONNECTED, CONNECTING, CONNECTED
    }

    data class State(
        val status: Status = Status.DISCONNECTED,
        val mode: String? = null,
        val torProgress: Int = 0
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state

    fun update(status: Status, mode: String? = null, torProgress: Int = 0) {
        _state.value = State(status, mode, torProgress)
    }

    fun updateTorProgress(progress: Int) {
        _state.value = _state.value.copy(torProgress = progress)
    }
}
