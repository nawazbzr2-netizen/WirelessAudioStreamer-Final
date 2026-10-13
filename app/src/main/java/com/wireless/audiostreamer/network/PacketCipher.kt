package com.wireless.audiostreamer.network

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class PacketCipher(pin: String) {
    private val key: SecretKeySpec
    private val rnd = SecureRandom()

    data class Opened(val type: Int, val session: Int, val seq: Int, val plain: ByteArray)

    init {
        val dig = MessageDigest.getInstance("SHA-256")
        val hash = dig.digest(("WAS|" + pin).toByteArray(Charsets.UTF_8))
        key = SecretKeySpec(hash, "AES")
    }

    fun seal(type: Int, session: Int, seq: Int, plain: ByteArray): ByteArray {
        val iv = ByteArray(12)
        rnd.nextBytes(iv)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        val enc = cipher.doFinal(plain)
        val out = ByteBuffer.allocate(Protocol.HEADER + 12 + enc.size).order(ByteOrder.LITTLE_ENDIAN)
        out.putInt(Protocol.MAGIC)
        out.put(type.toByte())
        out.put(0) // flags
        out.putInt(session)
        out.putInt(seq)
        out.putShort(plain.size.toShort())
        out.put(0) // reserved
        out.put(0)
        out.put(iv)
        out.put(enc)
        return out.array()
    }

    fun open(buf: ByteArray, len: Int): Opened? {
        if (len < Protocol.HEADER + 12 + 16) return null
        val bb = ByteBuffer.wrap(buf, 0, len).order(ByteOrder.LITTLE_ENDIAN)
        if (bb.int != Protocol.MAGIC) return null
        val type = bb.get().toInt() and 0xFF
        bb.get() // flags
        val session = bb.int
        val seq = bb.int
        val plainLen = bb.short.toInt() and 0xFFFF
        bb.get() // reserved
        bb.get()
        val iv = ByteArray(12)
        bb.get(iv)
        val encLen = len - Protocol.HEADER - 12
        if (encLen < 16) return null
        val enc = ByteArray(encLen)
        bb.get(enc)
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
            val plain = cipher.doFinal(enc)
            if (plain.size != plainLen) return null
            Opened(type, session, seq, plain)
        } catch (e: Exception) {
            null
        }
    }
}
