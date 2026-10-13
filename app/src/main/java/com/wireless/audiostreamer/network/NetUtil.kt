package com.wireless.audiostreamer.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface

object NetUtil {
    fun isPrivateIp(ip: String?): Boolean {
        if (ip.isNullOrBlank()) return false
        return try {
            val a = InetAddress.getByName(ip)
            a.isSiteLocalAddress || a.isLinkLocalAddress || a.isLoopbackAddress
        } catch (e: Exception) {
            false
        }
    }

    fun localIpv4(): String? {
        try {
            val en = NetworkInterface.getNetworkInterfaces() ?: return null
            while (en.hasMoreElements()) {
                val ni = en.nextElement()
                if (!ni.isUp || ni.isLoopback) continue
                val addrs = ni.inetAddresses
                while (addrs.hasMoreElements()) {
                    val a = addrs.nextElement()
                    if (a is Inet4Address && a.isSiteLocalAddress) return a.hostAddress
                }
            }
        } catch (e: Exception) {
        }
        return null
    }

    fun bindToWifi(ctx: Context, sock: DatagramSocket) {
        try {
            val cm = ctx.getSystemService(ConnectivityManager::class.java) ?: return
            val nets = cm.allNetworks
            for (n in nets) {
                val caps = cm.getNetworkCapabilities(n) ?: continue
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI_AWARE)
                ) {
                    n.bindSocket(sock)
                    return
                }
            }
        } catch (e: Exception) {
        }
    }

    fun wifiMulticastLock(ctx: Context): WifiManager.MulticastLock? {
        return try {
            val wm = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            wm.createMulticastLock("was-mcast").also { it.setReferenceCounted(false); it.acquire() }
        } catch (e: Exception) {
            null
        }
    }
}
