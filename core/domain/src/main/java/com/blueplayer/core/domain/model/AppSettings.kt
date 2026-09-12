package com.blueplayer.core.domain.model

enum class AudioFocusMode { PAUSE, DUCK, IGNORE }

enum class AccentColor(val displayName: String, val argb: Int) {
    DEFAULT("Default Blue", 0xFF1976D2.toInt()),
    OCEAN("Ocean", 0xFF0061A4.toInt()),
    TEAL("Teal", 0xFF00695C.toInt()),
    GREEN("Forest", 0xFF2E7D32.toInt()),
    ORANGE("Sunset", 0xFFE65100.toInt()),
    RED("Crimson", 0xFFC62828.toInt()),
    PINK("Rose", 0xFFAD1457.toInt()),
    PURPLE("Violet", 0xFF6A1B9A.toInt())
}

data class AppSettings(
    val dynamicColors: Boolean = false,
    val accentColor: AccentColor = AccentColor.DEFAULT,
    val crossfadeSeconds: Int = 0,
    val audioFocusMode: AudioFocusMode = AudioFocusMode.PAUSE,
    val keepScreenOn: Boolean = false,
    val queuePersistence: Boolean = true,
    val showWaveform: Boolean = true,
    val hapticFeedback: Boolean = true,
    val loudnessNormalization: Boolean = true
)