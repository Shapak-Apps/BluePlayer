package com.blueplayer.core.domain.backup

import com.blueplayer.core.domain.locale.AppLanguage
import com.blueplayer.core.domain.model.AccentColor
import com.blueplayer.core.domain.model.AppSettings
import com.blueplayer.core.domain.model.AudioFocusMode
import com.blueplayer.core.domain.model.CoverShape
import com.blueplayer.core.domain.model.SortOrder
import com.blueplayer.core.domain.model.TextSize
import com.blueplayer.core.domain.model.ThemeMode
import org.json.JSONArray
import org.json.JSONObject

/**
 * Full snapshot of user preferences that can be saved to / restored
 * from a JSON file. Theme and language are included so a backup
 * reproduces the exact look of the app.
 */
data class SettingsBackupData(
    val settings: AppSettings,
    val themeMode: ThemeMode,
    val language: AppLanguage
)

/**
 * Versioned JSON serializer for [SettingsBackupData].
 *
 * Format:
 * {
 *   "version": 1,
 *   "themeMode": "SYSTEM",
 *   "language": "RUSSIAN",
 *   "settings": { ... }
 * }
 *
 * Import rules:
 * - Unknown fields are ignored (forward compatibility).
 * - Missing fields fall back to current defaults (backward compatibility).
 * - Invalid enum names fall back to safe defaults instead of crashing.
 */
object SettingsBackup {

    const val CURRENT_VERSION = 1

    fun toJson(data: SettingsBackupData): String {
        val settings = data.settings
        val root = JSONObject()
        root.put("version", CURRENT_VERSION)
        root.put("themeMode", data.themeMode.name)
        root.put("language", data.language.name)

        val s = JSONObject()
        s.put("dynamicColors", settings.dynamicColors)
        s.put("accentColor", settings.accentColor.name)
        s.put("crossfadeSeconds", settings.crossfadeSeconds)
        s.put("audioFocusMode", settings.audioFocusMode.name)
        s.put("keepScreenOn", settings.keepScreenOn)
        s.put("queuePersistence", settings.queuePersistence)
        s.put("showWaveform", settings.showWaveform)
        s.put("hapticFeedback", settings.hapticFeedback)
        s.put("loudnessNormalization", settings.loudnessNormalization)
        s.put("minTrackDurationSeconds", settings.minTrackDurationSeconds)
        s.put("autoRescanOnLaunch", settings.autoRescanOnLaunch)
        s.put("onlineCoversEnabled", settings.onlineCoversEnabled)
        s.put("defaultSortOrder", settings.defaultSortOrder.name)
        s.put("textSize", settings.textSize.name)
        s.put("coverShape", settings.coverShape.name)

        val folders = JSONArray()
        settings.excludedFolders.forEach { folders.put(it) }
        s.put("excludedFolders", folders)

        root.put("settings", s)
        return root.toString(2)
    }

    fun fromJson(raw: String): SettingsBackupData? {
        return try {
            val root = JSONObject(raw)
            val version = root.optInt("version", 1)
            if (version < 1 || version > CURRENT_VERSION) return null

            val themeMode = enumOrDefault(
                root.optString("themeMode", ""), ThemeMode.SYSTEM
            )
            val language = enumOrDefault(
                root.optString("language", ""), AppLanguage.RUSSIAN
            )

            val s = root.optJSONObject("settings") ?: JSONObject()
            val defaults = AppSettings()

            val foldersArray = s.optJSONArray("excludedFolders")
            val folders = if (foldersArray != null) {
                val set = mutableSetOf<String>()
                for (i in 0 until foldersArray.length()) {
                    val value = foldersArray.optString(i, "")
                    if (value.isNotBlank()) set.add(value)
                }
                set
            } else {
                defaults.excludedFolders
            }

            val settings = AppSettings(
                dynamicColors = s.optBoolean("dynamicColors", defaults.dynamicColors),
                accentColor = enumOrDefault(s.optString("accentColor", ""), defaults.accentColor),
                crossfadeSeconds = s.optInt("crossfadeSeconds", defaults.crossfadeSeconds)
                    .coerceIn(0, 12),
                audioFocusMode = enumOrDefault(s.optString("audioFocusMode", ""), defaults.audioFocusMode),
                keepScreenOn = s.optBoolean("keepScreenOn", defaults.keepScreenOn),
                queuePersistence = s.optBoolean("queuePersistence", defaults.queuePersistence),
                showWaveform = s.optBoolean("showWaveform", defaults.showWaveform),
                hapticFeedback = s.optBoolean("hapticFeedback", defaults.hapticFeedback),
                loudnessNormalization = s.optBoolean("loudnessNormalization", defaults.loudnessNormalization),
                minTrackDurationSeconds = s.optInt("minTrackDurationSeconds", defaults.minTrackDurationSeconds)
                    .coerceIn(0, 120),
                excludedFolders = folders,
                autoRescanOnLaunch = s.optBoolean("autoRescanOnLaunch", defaults.autoRescanOnLaunch),
                onlineCoversEnabled = s.optBoolean("onlineCoversEnabled", defaults.onlineCoversEnabled),
                defaultSortOrder = enumOrDefault(s.optString("defaultSortOrder", ""), defaults.defaultSortOrder),
                textSize = enumOrDefault(s.optString("textSize", ""), defaults.textSize),
                coverShape = enumOrDefault(s.optString("coverShape", ""), defaults.coverShape)
            )

            SettingsBackupData(settings, themeMode, language)
        } catch (e: Exception) {
            null
        }
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(name: String, default: T): T {
        if (name.isBlank()) return default
        return runCatching { enumValueOf<T>(name) }.getOrDefault(default)
    }
}