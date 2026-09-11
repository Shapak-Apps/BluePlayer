package com.blueplayer.core.domain.model

enum class AudioFocusMode { PAUSE, DUCK, IGNORE }

enum class AccentColor(val displayName: String, val seed: Long) {
    DEFAULT("Default", 0x6750A4),
    BLUE("Ocean Blue", 0x0061A4),
    TEAL("Teal", 0x006B5E),
    GREEN("Forest", 0x386A1F),
    ORANGE("Sunset", 0xA43D00),
    RED("Crimson", 0xA52523),
    PINK("Rose", 0xA4346A),
    PURPLE("Violet", 0x6750A4)
}

data class AppSettings(
    val dynamicColors: Boolean = true,
    val accentColor: AccentColor = AccentColor.DEFAULT,
    val crossfadeSeconds: Int = 0,
    val audioFocusMode: AudioFocusMode = AudioFocusMode.PAUSE,
    val keepScreenOn: Boolean = false,
    val queuePersistence: Boolean = true,
    val showWaveform: Boolean = true,
    val hapticFeedback: Boolean = true
)