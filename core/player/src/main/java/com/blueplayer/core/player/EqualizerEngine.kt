package com.blueplayer.core.player

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer

data class BandInfo(
    val index: Int,
    val label: String,
    val min: Float,
    val max: Float,
    val level: Float
)

class EqualizerEngine {

    companion object {
        val PRESET_CURVES = mapOf(
            "Flat" to floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
            "Deep Bass" to floatArrayOf(60f, 55f, 45f, 30f, 15f, 0f, 0f, 0f, 0f, 0f),
            "Bass Boost" to floatArrayOf(50f, 40f, 25f, 10f, 0f, 0f, 0f, 0f, 0f, 0f),
            "Treble" to floatArrayOf(0f, 0f, 0f, 10f, 25f, 40f, 50f, 55f, 60f, 60f),
            "Vocal" to floatArrayOf(-20f, -5f, 20f, 45f, 50f, 40f, 20f, 5f, 0f, -10f),
            "Rock" to floatArrayOf(45f, 30f, 10f, 0f, -5f, 0f, 20f, 35f, 45f, 50f),
            "Pop" to floatArrayOf(-5f, 10f, 30f, 40f, 40f, 25f, 10f, 0f, -5f, -5f),
            "Jazz" to floatArrayOf(25f, 15f, 0f, -5f, -5f, 5f, 15f, 25f, 35f, 35f),
            "Classical" to floatArrayOf(30f, 20f, 10f, 0f, -5f, -5f, 0f, 15f, 25f, 30f),
            "Dance" to floatArrayOf(50f, 40f, 25f, 10f, 0f, -5f, 10f, 20f, 35f, 45f),
            "Hip-Hop" to floatArrayOf(50f, 40f, 25f, 15f, 5f, 0f, -5f, -5f, 5f, 15f),
            "Electronic" to floatArrayOf(55f, 45f, 25f, 5f, -10f, -5f, 10f, 30f, 45f, 55f),
            "Metal" to floatArrayOf(40f, 25f, 10f, 0f, -5f, 0f, 20f, 40f, 50f, 55f),
            "Acoustic" to floatArrayOf(20f, 15f, 5f, 0f, 10f, 20f, 30f, 25f, 15f, 5f),
            "Loudness" to floatArrayOf(40f, 30f, 10f, 0f, -5f, 0f, 10f, 20f, 35f, 45f)
        )
    }

    private var eq: Equalizer? = null
    private var bass: BassBoost? = null
    private var virt: Virtualizer? = null
    private var loud: LoudnessEnhancer? = null
    private var reverb: PresetReverb? = null
    private var attachedSession = 0

    var isEnabled = true
        private set
    var virtStrength = 0f
        private set
    var preGainDb = 0f
        private set
    var bassBoostPercent = 0f
        private set
    var reverbPreset = PresetReverb.PRESET_NONE
        private set

    val isAvailable: Boolean get() = eq != null

    fun attach(sessionId: Int) {
        if (sessionId == 0) return
        if (sessionId == attachedSession && eq != null) return

        release()
        attachedSession = sessionId

        eq = runCatching { Equalizer(0, sessionId) }.getOrNull()
        bass = runCatching { BassBoost(0, sessionId) }.getOrNull()
        virt = runCatching { Virtualizer(0, sessionId) }.getOrNull()
        loud = runCatching { LoudnessEnhancer(sessionId) }.getOrNull()
        reverb = runCatching { PresetReverb(0, sessionId) }.getOrNull()

        eq?.enabled = isEnabled
        bass?.enabled = isEnabled
        virt?.enabled = isEnabled
        reverb?.enabled = isEnabled

        BassDsp.enabled = isEnabled
        applyBassToNative()
    }

    fun setEnabled(on: Boolean) {
        isEnabled = on
        runCatching { eq?.enabled = on }
        runCatching { bass?.enabled = on }
        runCatching { virt?.enabled = on }
        runCatching { reverb?.enabled = on }
        BassDsp.enabled = on
    }

    fun setPreGain(db: Float) {
        preGainDb = db.coerceIn(-15f, 15f)
        runCatching {
            loud?.setTargetGain((preGainDb.coerceIn(0f, 6f) * 100).toInt())
        }
    }

    fun readBands(): List<BandInfo> {
        val n = runCatching { eq?.numberOfBands?.toInt() ?: 0 }.getOrDefault(0)
        val range = runCatching { eq?.bandLevelRange }.getOrNull()
        val min = range?.get(0)?.toFloat() ?: -1500f
        val max = range?.get(1)?.toFloat() ?: 1500f

        return (0 until n).map { i ->
            BandInfo(
                index = i,
                label = freqLabel(centerFreqHz(i)),
                min = min,
                max = max,
                level = runCatching { eq?.getBandLevel(i.toShort())?.toFloat() ?: 0f }.getOrDefault(0f)
            )
        }
    }

    fun setBandLevel(index: Int, level: Float) {
        runCatching { eq?.setBandLevel(index.toShort(), level.toInt().toShort()) }
    }

    fun setBassBoost(percent: Float) {
        bassBoostPercent = percent.coerceIn(0f, 100f)
        applyBassToNative()
        val strength = (bassBoostPercent * 5).toInt().coerceIn(0, 500)
        runCatching { bass?.setStrength(strength.toShort()) }
    }

    fun setVirtualizer(value: Float) {
        virtStrength = value.coerceIn(0f, 1000f)
        runCatching { virt?.setStrength(virtStrength.toInt().toShort()) }
    }

    fun setReverb(preset: Short) {
        reverbPreset = preset
        runCatching {
            reverb?.preset = preset
            reverb?.enabled = preset != PresetReverb.PRESET_NONE
        }
    }

    fun applyPreset(name: String): List<BandInfo> {
        val curve = PRESET_CURVES[name] ?: return readBands()
        val bands = readBands()
        if (bands.isEmpty()) return bands

        bands.forEach { band ->
            val t = if (bands.size > 1) band.index.toFloat() / (bands.size - 1) else 0.5f
            val pct = sampleCurve(curve, t)
            val span = band.max - band.min
            val level = band.min + span * (pct + 100f) / 200f
            setBandLevel(band.index, level)
        }

        return readBands()
    }

    fun release() {
        runCatching { eq?.release() }
        runCatching { bass?.release() }
        runCatching { virt?.release() }
        runCatching { loud?.release() }
        runCatching { reverb?.release() }
        eq = null; bass = null; virt = null; loud = null; reverb = null
        attachedSession = 0
    }

    private fun applyBassToNative() {
        BassDsp.gainDb = (bassBoostPercent / 100f) * 12f
    }

    private fun centerFreqHz(band: Int): Int =
        runCatching { (eq?.getCenterFreq(band.toShort()) ?: 0) / 1000 }.getOrDefault(0)

    private fun freqLabel(hz: Int): String =
        if (hz >= 1000) "%.1fk".format(hz / 1000f) else "${hz}Hz"

    private fun sampleCurve(curve: FloatArray, t: Float): Float {
        val pos = t * (curve.size - 1)
        val i0 = pos.toInt().coerceIn(0, curve.size - 2)
        val frac = pos - i0
        return curve[i0] + (curve[i0 + 1] - curve[i0]) * frac
    }
}