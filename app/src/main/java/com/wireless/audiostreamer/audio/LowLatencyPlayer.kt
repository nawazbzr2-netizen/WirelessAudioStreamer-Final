package com.wireless.audiostreamer.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.concurrent.thread

class LowLatencyPlayer(private val jitter: JitterBuffer) {
    @Volatile private var running = false
    @Volatile private var channels = 1
    private var track: AudioTrack? = null
    private var worker: Thread? = null

    fun start(ch: Int) {
        if (running) return
        channels = ch
        running = true
        val chMask = if (ch == 2) AudioFormat.CHANNEL_OUT_STEREO else AudioFormat.CHANNEL_OUT_MONO
        val minBuf = AudioTrack.getMinBufferSize(
            SystemAudioCapturer.SAMPLE_RATE, chMask, AudioFormat.ENCODING_PCM_16BIT
        )
        val bufSize = maxOf(minBuf, SystemAudioCapturer.FRAME_SAMPLES * ch * 2 * 8)
        val at = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SystemAudioCapturer.SAMPLE_RATE)
                    .setChannelMask(chMask)
                    .build()
            )
            .setBufferSizeInBytes(bufSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .build()
        track = at
        at.play()
        worker = thread(isDaemon = true, name = "was-player") {
            val silence = ShortArray(SystemAudioCapturer.FRAME_SAMPLES * channels)
            while (running) {
                val frame = jitter.poll()
                if (frame != null) {
                    at.write(frame, 0, frame.size)
                } else {
                    at.write(silence, 0, silence.size)
                    try { Thread.sleep(2) } catch (e: InterruptedException) { return@thread }
                }
            }
        }
    }

    fun setChannels(ch: Int) {
        if (ch == channels) return
        stop()
        start(ch)
    }

    fun stop() {
        running = false
        try { worker?.join(500) } catch (e: Exception) {}
        worker = null
        try {
            track?.stop()
            track?.release()
        } catch (e: Exception) {}
        track = null
    }
}
