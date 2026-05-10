package com.example.wl.vpn

class TorVpnService : BaseVpnService() {
    override fun getNotificationTitle(): String = "WL VPN: Tor"
    override fun getModeName(): String = "tor"
}
