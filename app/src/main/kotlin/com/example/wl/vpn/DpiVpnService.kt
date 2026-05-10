package com.example.wl.vpn

class DpiVpnService : BaseVpnService() {
    override fun getNotificationTitle(): String = "WL VPN: ByeByeDPI"
    override fun getModeName(): String = "dpi"
}
