package com.blueplayer.app.audio

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import com.blueplayer.core.player.BassDsp
import java.nio.ByteBuffer
import java.nio.ByteOrder

@OptIn(UnstableApi::class)
class NativeBassProcessor : AudioProcessor {

    private var handle = 0L
    private var outputArray = ShortArray(0)
    private var outputBuffer = ByteBuffer.allocate(0).order(ByteOrder.nativeOrder())
    private var inputEnded = false
    private var lastGain = Float.NaN

    override fun configure(
        inputAudioFormat: AudioProcessor.AudioFormat
    ): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        releaseHandle()
        handle = nativeCreate(inputAudioFormat.sampleRate, inputAudioFormat.channelCount)
        outputArray = ShortArray(INITIAL_SAMPLES)
        outputBuffer = ByteBuffer.allocate(INITIAL_SAMPLES * 2).order(ByteOrder.nativeOrder())
        outputBuffer.limit(0)
        inputEnded = false
        lastGain = Float.NaN
        return inputAudioFormat
    }

    override fun isActive(): Boolean = handle != 0L

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0 || handle == 0L) return

        val samples = remaining / 2
        val src = ShortArray(samples)
        inputBuffer.duplicate().order(ByteOrder.nativeOrder()).asShortBuffer().get(src)
        inputBuffer.position(inputBuffer.limit())

        if (samples > outputArray.size) {
            outputArray = ShortArray(samples)
            outputBuffer = ByteBuffer.allocate(samples * 2).order(ByteOrder.nativeOrder())
        }

        val gain = if (BassDsp.enabled) BassDsp.gainDb else 0f
        if (gain != lastGain) {
            nativeSetGain(handle, gain)
            lastGain = gain
        }

        nativeProcess(handle, src, outputArray, samples)

        outputBuffer.clear()
        outputBuffer.asShortBuffer().put(outputArray, 0, samples)
        outputBuffer.position(0)
        outputBuffer.limit(samples * 2)
    }

    override fun queueEndOfStream() {
        inputEnded = true
    }

    override fun getOutput(): ByteBuffer = outputBuffer

    override fun isEnded(): Boolean = inputEnded && outputBuffer.remaining() == 0

    override fun flush() {
        if (handle != 0L) nativeFlush(handle)
        inputEnded = false
        outputBuffer.clear()
        outputBuffer.limit(0)
    }

    override fun reset() {
        flush()
        lastGain = Float.NaN
    }

    fun release() {
        releaseHandle()
    }

    private fun releaseHandle() {
        if (handle != 0L) {
            nativeDestroy(handle)
            handle = 0L
        }
    }

    private companion object {
        const val INITIAL_SAMPLES = 32768

        init {
            System.loadLibrary("bassdsp")
        }
    }

    private external fun nativeCreate(sampleRate: Int, channels: Int): Long
    private external fun nativeDestroy(handle: Long)
    private external fun nativeSetGain(handle: Long, db: Float)
    private external fun nativeFlush(handle: Long)
    private external fun nativeProcess(handle: Long, input: ShortArray, output: ShortArray, samples: Int)
}