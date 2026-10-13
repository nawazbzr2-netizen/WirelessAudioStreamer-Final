package com.wireless.audiostreamer.network

import java.nio.ByteBuffer
import java.nio.ByteOrder

object Protocol {
    const val PORT_DISCOVERY = 48765
    const val PORT_AUDIO = 48766
    const val T_ANNOUNCE = 2
    const val T_AUDIO = 10

    data class AudioFrame(val pcm: ShortArray, val channels: Int)

    fun buildAudioPlain(pcm: ShortArray, channels: Int): ByteArray {
        val bb = ByteBuffer.allocate(2 + pcm.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        bb.put(channels.toByte())
        bb.put(0)
        for (s in pcm) bb.putShort(s)
        return bb.array()
    }

    fun parseAudioPlain(plain: ByteArray): AudioFrame? {
        if (plain.size < 2) return null
        val channels = plain[0].toInt() and 0xFF
        if (channels !in 1..2) return null
        val samples = (plain.size - 2) / 2
        if (samples <= 0) return null
        val pcm = ShortArray(samples)
        val bb = ByteBuffer.wrap(plain, 2, plain.size - 2).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until samples) pcm[i] = bb.short
        return AudioFrame(pcm, channels)
    }
}
