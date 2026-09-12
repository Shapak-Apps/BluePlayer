package com.blueplayer.core.data.prefs

import android.content.Context
import android.content.SharedPreferences
import com.blueplayer.core.domain.model.AccentColor
import com.blueplayer.core.domain.model.AppSettings
import com.blueplayer.core.domain.model.AudioFocusMode
import com.blueplayer.core.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class SharedPreferencesSettingsRepository(
    context: Context
) : SettingsRepository {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    private val legacyPrefs: SharedPreferences =
        context.getSharedPreferences("blue_player_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(load())
    override val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun load(): AppSettings = AppSettings(
        dynamicColors = prefs.getBoolean("dynamic_colors", false),
        accentColor = runCatching {
            AccentColor.valueOf(prefs.getString("accent_color", "DEFAULT") ?: "DEFAULT")
        }.getOrDefault(AccentColor.DEFAULT),
        crossfadeSeconds = prefs.getInt("crossfade_seconds", 0),
        audioFocusMode = runCatching {
            AudioFocusMode.valueOf(prefs.getString("audio_focus", "PAUSE") ?: "PAUSE")
        }.getOrDefault(AudioFocusMode.PAUSE),
        keepScreenOn = prefs.getBoolean("keep_screen_on", false),
        queuePersistence = prefs.getBoolean("queue_persistence", true),
        showWaveform = prefs.getBoolean("show_waveform", true),
        hapticFeedback = prefs.getBoolean("haptic_feedback", true),
        loudnessNormalization = legacyPrefs.getBoolean("sound_normalize", true)
    )

    override suspend fun update(update: (AppSettings) -> AppSettings) =
        withContext(Dispatchers.IO) {
            val new = update(_settings.value)
            prefs.edit()
                .putBoolean("dynamic_colors", new.dynamicColors)
                .putString("accent_color", new.accentColor.name)
                .putInt("crossfade_seconds", new.crossfadeSeconds)
                .putString("audio_focus", new.audioFocusMode.name)
                .putBoolean("keep_screen_on", new.keepScreenOn)
                .putBoolean("queue_persistence", new.queuePersistence)
                .putBoolean("show_waveform", new.showWaveform)
                .putBoolean("haptic_feedback", new.hapticFeedback)
                .putBoolean("loudness_normalization", new.loudnessNormalization)
                .apply()
            legacyPrefs.edit()
                .putBoolean("sound_normalize", new.loudnessNormalization)
                .apply()
            _settings.value = new
        }

    override suspend fun resetToDefaults() {
        update { AppSettings() }
    }
}