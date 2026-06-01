package com.dogear.reader.feature.upload.server

import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * Finds the device's local-network IPv4 address (e.g. 192.168.x.x). The upload server binds to
 * this address so it is reachable only on the LAN, not loopback or cellular — part of the
 * "local network only" requirement (Security Review §E).
 */
internal object Network {

    fun localIpAddress(): String? = runCatching {
        NetworkInterface.getNetworkInterfaces().asSequence()
            .filter { it.isUp && !it.isLoopback && !it.isVirtual }
            .flatMap { it.inetAddresses.asSequence() }
            .filterIsInstance<Inet4Address>()
            .firstOrNull { it.isSiteLocalAddress }
            ?.hostAddress
    }.getOrNull()
}
