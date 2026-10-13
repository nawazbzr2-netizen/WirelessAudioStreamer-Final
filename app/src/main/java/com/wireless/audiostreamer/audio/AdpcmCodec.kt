package com.wireless.audiostreamer.audio

/** Placeholder: currently passes PCM through. Can be swapped for real ADPCM later. */
object AdpcmCodec {
    fun encode(pcm: ShortArray): ByteArray {
        val out = ByteArray(pcm.size * 2)
        for (i in pcm.indices) {
            val v = pcm[i].toInt()
            out[i * 2] = (v and 0xFF).toByte()
            out[i * 2 + 1] = ((v shr 8) and 0xFF).toByte()
        }
        return out
    }

    fun decode(data: ByteArray, offset: Int, len: Int): ShortArray {
        val samples = len / 2
        val pcm = ShortArray(samples)
        for (i in 0 until samples) {
            val lo = data[offset + i * 2].toInt() and 0xFF
            val hi = data[offset + i * 2 + 1].toInt() and 0xFF
            pcm[i] = ((hi shl 8) or lo).toShort()
        }
        return pcm
    }
}
