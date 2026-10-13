package com.wireless.audiostreamer.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection
import android.os.Build
import kotlin.concurrent.thread

class SystemAudioCapturer(
    private val projection: MediaProjection,
    private val channels: Int,
    private val onPcm: (ShortArray) -> Unit
) {
    @Volatile private var running = false
    private var record: AudioRecord? = null
    private var worker: Thread? = null

    fun start() {
        if (running) return
        running = true
        val chMask = if (channels == 2) AudioFormat.CHANNEL_IN_STEREO else AudioFormat.CHANNEL_IN_MONO
        val cfg = AudioPlaybackCaptureConfiguration.Builder(projection)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            .addMatchingUsage(AudioAttributes.USAGE_GAME)
            .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
            .build()
        val format = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(SAMPLE_RATE)
            .setChannelMask(chMask)
            .build()
        val minBuf = AudioRecord.getMinBufferSize(SAMPLE_RATE, chMask, AudioFormat.ENCODING_PCM_16BIT)
        val bufSize = maxOf(minBuf, FRAME_SAMPLES * channels * 2 * 4)
        val ar = AudioRecord.Builder()
            .setAudioFormat(format)
            .setBufferSizeInBytes(bufSize)
            .setAudioPlaybackCaptureConfig(cfg)
            .build()
        if (ar.state != AudioRecord.STATE_INITIALIZED) {
            ar.release()
            running = false
            return
        }
        record = ar
        ar.startRecording()
        worker = thread(isDaemon = true, name = "was-capture") {
            val shorts = ShortArray(FRAME_SAMPLES * channels)
            while (running) {
                val n = ar.read(shorts, 0, shorts.size)
                if (n > 0) {
                    val frame = if (n == shorts.size) shorts.clone() else shorts.copyOf(n)
                    onPcm(frame)
                } else if (n < 0) {
                    break
                }
            }
        }
    }

    fun stop() {
        running = false
        try { worker?.join(500) } catch (e: Exception) {}
        worker = null
        try {
            record?.stop()
            record?.release()
        } catch (e: Exception) {}
        record = null
    }

    companion object {
        const val SAMPLE_RATE = 48000
        const val FRAME_SAMPLES = 480 // 10 ms
    }
}
