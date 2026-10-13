package com.wireless.audiostreamer.network

import android.content.Context
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import kotlin.concurrent.thread

class DiscoveryHelper(private val ctx: Context, private val cipher: PacketCipher) {
    data class Peer(val ip: String, val name: String, val role: String, var lastSeen: Long)

    private val peers = LinkedHashMap<String, Peer>()
    @Volatile private var running = false
    private var sock: DatagramSocket? = null
    private var mcastLock: android.net.wifi.WifiManager.MulticastLock? = null
    private var myName = ""
    private var myRole = ""
    private var myExtra = ""

    fun start(name: String, role: String, extra: String) {
        myName = name
        myRole = role
        myExtra = extra
        if (running) return
        running = true
        mcastLock = NetUtil.wifiMulticastLock(ctx)
        thread(isDaemon = true, name = "was-disc-rx") { rxLoop() }
        thread(isDaemon = true, name = "was-disc-tx") { txLoop() }
    }

    fun stop() {
        running = false
        try { sock?.close() } catch (e: Exception) {}
        sock = null
        try { mcastLock?.release() } catch (e: Exception) {}
        mcastLock = null
        synchronized(peers) { peers.clear() }
    }

    fun snapshot(): List<Peer> = synchronized(peers) {
        val now = System.currentTimeMillis()
        peers.values.filter { now - it.lastSeen < 8000 }.toList()
    }

    fun peer(ip: String): Peer? = synchronized(peers) { peers[ip] }

    fun lastSeen(ip: String): Long = synchronized(peers) { peers[ip]?.lastSeen ?: 0L }

    private fun rxLoop() {
        val buf = ByteArray(1024)
        while (running) {
            var s: DatagramSocket? = null
            try {
                s = DatagramSocket(null)
                s.reuseAddress = true
                s.broadcast = true
                NetUtil.bindToWifi(ctx, s)
                s.bind(InetSocketAddress(Protocol.PORT_DISCOVERY))
                sock = s
                while (running) {
                    val p = DatagramPacket(buf, buf.size)
                    s.receive(p)
                    val from = p.address?.hostAddress ?: continue
                    if (!NetUtil.isPrivateIp(from)) continue
                    handleAnnounce(buf, p.length, from)
                }
            } catch (e: Exception) {
                if (running) try { Thread.sleep(500) } catch (ie: InterruptedException) { return }
            } finally {
                try { s?.close() } catch (e: Exception) {}
                if (sock === s) sock = null
            }
        }
    }

    private fun handleAnnounce(buf: ByteArray, len: Int, from: String) {
        val o = cipher.open(buf, len) ?: return
        if (o.type != Protocol.T_ANNOUNCE) return
        val plain = o.plain
        if (plain.size < 3) return
        val roleLen = plain[0].toInt() and 0xFF
        if (roleLen <= 0 || 1 + roleLen >= plain.size) return
        val role = String(plain, 1, roleLen, StandardCharsets.UTF_8)
        val nameStart = 1 + roleLen
        val nameLen = plain[nameStart].toInt() and 0xFF
        if (nameStart + 1 + nameLen > plain.size) return
        val name = String(plain, nameStart + 1, nameLen, StandardCharsets.UTF_8)
        synchronized(peers) {
            peers[from] = Peer(from, name, role, System.currentTimeMillis())
        }
    }

    private fun txLoop() {
        while (running) {
            try {
                announce()
            } catch (e: Exception) {
            }
            try { Thread.sleep(1500) } catch (e: InterruptedException) { return }
        }
    }

    private fun announce() {
        val roleB = myRole.toByteArray(StandardCharsets.UTF_8)
        val nameB = myName.toByteArray(StandardCharsets.UTF_8)
        val plain = ByteArray(1 + roleB.size + 1 + nameB.size)
        plain[0] = roleB.size.toByte()
        System.arraycopy(roleB, 0, plain, 1, roleB.size)
        plain[1 + roleB.size] = nameB.size.toByte()
        System.arraycopy(nameB, 0, plain, 2 + roleB.size, nameB.size)
        val frame = cipher.seal(Protocol.T_ANNOUNCE, 0, 0, plain)
        val s = DatagramSocket()
        try {
            s.broadcast = true
            NetUtil.bindToWifi(ctx, s)
            val bcast = InetAddress.getByName("255.255.255.255")
            s.send(DatagramPacket(frame, frame.size, bcast, Protocol.PORT_DISCOVERY))
            val local = NetUtil.localIpv4()
            if (local != null) {
                val parts = local.split(".")
                if (parts.size == 4) {
                    val sub = "${parts[0]}.${parts[1]}.${parts[2]}.255"
                    s.send(DatagramPacket(frame, frame.size, InetAddress.getByName(sub), Protocol.PORT_DISCOVERY))
                }
            }
        } finally {
            s.close()
        }
    }
}
