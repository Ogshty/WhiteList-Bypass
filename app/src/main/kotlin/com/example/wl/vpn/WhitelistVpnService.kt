package com.example.wl.vpn

class WhitelistVpnService : BaseVpnService() {
    override fun getNotificationTitle(): String = "WL VPN: Whitelist"
    override fun getModeName(): String = "whitelist"
}
