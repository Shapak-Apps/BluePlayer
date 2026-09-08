package com.blueplayer.core.data.prefs

import android.content.Context
import com.blueplayer.core.domain.model.ThemeMode
import com.blueplayer.core.domain.repository.ThemeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SharedPreferencesThemeRepository(
    context: Context
) : ThemeRepository {

    private companion object {
        const val PREFS_NAME = "blue_player_settings"
        const val KEY_THEME_MODE = "theme_mode"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(loadThemeMode())
    override val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    override fun setThemeMode(mode: ThemeMode) {
        prefs.edit()
            .putString(KEY_THEME_MODE, mode.name)
            .apply()

        _themeMode.value = mode
    }

    private fun loadThemeMode(): ThemeMode {
        val saved = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)

        return ThemeMode.entries.firstOrNull { it.name == saved }
            ?: ThemeMode.SYSTEM
    }
}