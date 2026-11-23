package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * Lightweight procedural audio synthesizer for realistic Bangladeshi bus air horn,
 * pneumatic door chime, and subtle collision thud without external sound asset dependencies.
 */
class BusAudioEngine {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var hornJob: Job? = null
    @Volatile
    var isEnabled: Boolean = true

    fun triggerHorn(active: Boolean) {
        if (!isEnabled) {
            hornJob?.cancel()
            hornJob = null
            return
        }
        if (active) {
            if (hornJob?.isActive == true) return
            hornJob = scope.launch {
                playAirHornLoop()
            }
        } else {
            hornJob?.cancel()
            hornJob = null
        }
    }

    fun playDoorChime() {
        if (!isEnabled) return
        scope.launch {
            playToneSequence(listOf(660.0 to 110, 880.0 to 150), amplitude = 0.28)
        }
    }

    fun playBrakeHiss() {
        if (!isEnabled) return
        scope.launch {
            playNoiseBurst(durationMs = 140, amplitude = 0.14)
        }
    }

    fun playBump() {
        if (!isEnabled) return
        scope.launch {
            playToneSequence(listOf(110.0 to 90, 75.0 to 110), amplitude = 0.35)
        }
    }

    private fun playAirHornLoop() {
        val sampleRate = 22050
        val chunkSamples = sampleRate / 10
        val buffer = ShortArray(chunkSamples)
        var track: AudioTrack? = null
        try {
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(chunkSamples * 2)

            track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBuf)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            track.play()
            var sampleIndex = 0L
            // Classic Bangladeshi highway coach dual-tone air horn (F4 + A4 + harmonics)
            val f1 = 349.23
            val f2 = 440.00
            while (scope.isActive && hornJob?.isActive == true && isEnabled) {
                for (i in 0 until chunkSamples) {
                    val t = sampleIndex.toDouble() / sampleRate
                    val wave = 0.45 * sin(2.0 * PI * f1 * t) +
                        0.35 * sin(2.0 * PI * f2 * t) +
                        0.15 * sin(2.0 * PI * f1 * 2.0 * t) +
                        0.05 * sin(2.0 * PI * f2 * 2.0 * t)
                    buffer[i] = (wave * 0.42 * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                    sampleIndex++
                }
                track.write(buffer, 0, chunkSamples)
            }
        } catch (_: Throwable) {
            // Audio device unavailable in headless/test environments
        } finally {
            runCatching {
                track?.stop()
                track?.release()
            }
        }
    }

    private fun playToneSequence(notes: List<Pair<Double, Int>>, amplitude: Double) {
        val sampleRate = 22050
        var track: AudioTrack? = null
        try {
            val totalSamples = notes.sumOf { (sampleRate * it.second) / 1000 }
            val buffer = ShortArray(totalSamples)
            var idx = 0
            for ((freq, ms) in notes) {
                val count = (sampleRate * ms) / 1000
                for (i in 0 until count) {
                    val env = if (i < 200) i / 200.0 else if (i > count - 300) (count - i) / 300.0 else 1.0
                    val s = sin(2.0 * PI * freq * i / sampleRate) * amplitude * env
                    buffer[idx++] = (s * Short.MAX_VALUE).toInt().toShort()
                }
            }
            track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes((totalSamples * 2).coerceAtLeast(2048))
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track.write(buffer, 0, buffer.size)
            track.play()
            Thread.sleep(notes.sumOf { it.second.toLong() } + 40L)
        } catch (_: Throwable) {
        } finally {
            runCatching {
                track?.stop()
                track?.release()
            }
        }
    }

    private fun playNoiseBurst(durationMs: Int, amplitude: Double) {
        val sampleRate = 22050
        val count = (sampleRate * durationMs) / 1000
        var track: AudioTrack? = null
        try {
            val buffer = ShortArray(count)
            var seed = 1337
            for (i in 0 until count) {
                seed = (seed * 1103515245 + 12345) and 0x7fffffff
                val noise = (seed.toDouble() / Int.MAX_VALUE) * 2.0 - 1.0
                val decay = 1.0 - (i.toDouble() / count)
                buffer[i] = (noise * amplitude * decay * Short.MAX_VALUE).toInt().toShort()
            }
            track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes((count * 2).coerceAtLeast(2048))
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track.write(buffer, 0, buffer.size)
            track.play()
            Thread.sleep(durationMs.toLong() + 30L)
        } catch (_: Throwable) {
        } finally {
            runCatching {
                track?.stop()
                track?.release()
            }
        }
    }

    fun release() {
        hornJob?.cancel()
        scope.cancel()
    }
}
