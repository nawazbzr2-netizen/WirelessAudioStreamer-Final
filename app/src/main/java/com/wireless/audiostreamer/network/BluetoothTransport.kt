package com.wireless.audiostreamer.network

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.UUID
import kotlin.concurrent.thread

class BluetoothTransport(private val adapter: BluetoothAdapter) {
    interface Listener {
        fun onState(connected: Boolean)
        fun onFrame(buf: ByteArray, len: Int)
    }

    private val uuid = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    @Volatile private var running = false
    private var server: BluetoothServerSocket? = null
    private var socket: BluetoothSocket? = null
    private var out: DataOutputStream? = null
    private var listener: Listener? = null

    fun startServer(l: Listener) {
        listener = l
        running = true
        thread(isDaemon = true, name = "was-bt-server") {
            try {
                val ss = adapter.listenUsingRfcommWithServiceRecord("WAS", uuid)
                server = ss
                val s = ss.accept()
                if (!running) {
                    s.close()
                    return@thread
                }
                attach(s)
            } catch (e: Exception) {
                if (running) listener?.onState(false)
            }
        }
    }

    fun connect(device: BluetoothDevice, l: Listener) {
        listener = l
        running = true
        thread(isDaemon = true, name = "was-bt-client") {
            try {
                val s = device.createRfcommSocketToServiceRecord(uuid)
                adapter.cancelDiscovery()
                s.connect()
                if (!running) {
                    s.close()
                    return@thread
                }
                attach(s)
            } catch (e: Exception) {
                if (running) listener?.onState(false)
            }
        }
    }

    @Synchronized
    private fun attach(s: BluetoothSocket) {
        socket = s
        out = DataOutputStream(s.outputStream)
        listener?.onState(true)
        thread(isDaemon = true, name = "was-bt-rx") {
            try {
                val inp = DataInputStream(s.inputStream)
                val buf = ByteArray(8192)
                while (running) {
                    val len = inp.readInt()
                    if (len <= 0 || len > buf.size) break
                    inp.readFully(buf, 0, len)
                    listener?.onFrame(buf, len)
                }
            } catch (e: Exception) {
            } finally {
                close()
                listener?.onState(false)
            }
        }
    }

    @Synchronized
    fun send(frame: ByteArray) {
        try {
            val o = out ?: return
            o.writeInt(frame.size)
            o.write(frame)
            o.flush()
        } catch (e: Exception) {
            close()
        }
    }

    @Synchronized
    fun close() {
        running = false
        try { out?.close() } catch (e: Exception) {}
        try { socket?.close() } catch (e: Exception) {}
        try { server?.close() } catch (e: Exception) {}
        out = null
        socket = null
        server = null
    }
}
