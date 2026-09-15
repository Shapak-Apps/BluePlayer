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

enum class SortOrder(val displayNameResKey: String) {
    TITLE_ASC("sortTitleAsc"),
    TITLE_DESC("sortTitleDesc"),
    DATE_ADDED_DESC("sortDateDesc"),
    DATE_ADDED_ASC("sortDateAsc"),
    DURATION_ASC("sortDurationAsc"),
    DURATION_DESC("sortDurationDesc"),
    ARTIST_ASC("sortArtistAsc"),
    ALBUM_ASC("sortAlbumAsc")
}

enum class TextSize(val scale: Float) {
    SMALL(0.9f),
    NORMAL(1.0f),
    LARGE(1.15f)
}

enum class CoverShape { ROUNDED, CIRCLE, SQUARE }

data class AppSettings(
    val dynamicColors: Boolean = false,
    val accentColor: AccentColor = AccentColor.DEFAULT,
    val crossfadeSeconds: Int = 0,
    val audioFocusMode: AudioFocusMode = AudioFocusMode.PAUSE,
    val keepScreenOn: Boolean = false,
    val queuePersistence: Boolean = true,
    val showWaveform: Boolean = true,
    val hapticFeedback: Boolean = true,
    val loudnessNormalization: Boolean = true,
    val minTrackDurationSeconds: Int = 30,
    val excludedFolders: Set<String> = setOf("Ringtones", "Notifications", "Alarms"),
    val autoRescanOnLaunch: Boolean = true,
    val onlineCoversEnabled: Boolean = true,
    val defaultSortOrder: SortOrder = SortOrder.TITLE_ASC,
    val textSize: TextSize = TextSize.NORMAL,
    val coverShape: CoverShape = CoverShape.ROUNDED
)