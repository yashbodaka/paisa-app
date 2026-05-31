package com.paisa.app.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

/**
 * Captures raw 16kHz mono PCM audio from the microphone.
 * Whisper requires 16kHz, 16-bit signed integer, mono audio.
 *
 * Auto-stops after 1.5 seconds of trailing silence so the user
 * doesn't have to tap the mic button again for short phrases.
 */
object AudioCapture {

    const val SAMPLE_RATE = 16_000
    private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

    // Silence detection: stop if RMS stays below threshold for this many chunks
    private const val SILENCE_RMS = 0.008f      // normalised (0-1)
    private const val SILENCE_CHUNKS = 12       // ~1.5 s at chunk size ≈ 0.125 s

    @SuppressLint("MissingPermission")
    suspend fun record(
        maxSeconds: Int = 30,                    // 30 s for longer logs
        stopSignal: () -> Boolean
    ): FloatArray = withContext(Dispatchers.IO) {

        val minBuf = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        // Chunk ≈ 0.125 s of audio
        val chunkSamples = SAMPLE_RATE / 8
        val bufferSize = maxOf(minBuf, chunkSamples * 2)

        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, bufferSize
        )

        val collected = mutableListOf<Short>()
        val chunk = ShortArray(chunkSamples)
        val maxSamples = SAMPLE_RATE * maxSeconds
        var silenceRun = 0          // consecutive silent chunks
        var hasSpeech = false       // don't stop on leading silence

        try {
            recorder.startRecording()
            while (isActive && !stopSignal() && collected.size < maxSamples) {
                val read = recorder.read(chunk, 0, chunk.size)
                if (read > 0) {
                    // RMS of this chunk
                    var sumSq = 0.0
                    repeat(read) { sumSq += (chunk[it] / 32768.0).let { v -> v * v } }
                    val rms = sqrt(sumSq / read).toFloat()

                    if (rms > SILENCE_RMS) {
                        hasSpeech = true
                        silenceRun = 0
                    } else if (hasSpeech) {
                        silenceRun++
                        if (silenceRun >= SILENCE_CHUNKS) break   // trailing silence → stop
                    }

                    repeat(read) { collected.add(chunk[it]) }
                }
            }
        } finally {
            recorder.stop()
            recorder.release()
        }

        FloatArray(collected.size) { i -> collected[i] / 32768f }
    }
}
