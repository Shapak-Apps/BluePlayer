package com.blueplayer.core.data.prefs

import android.content.Context
import com.blueplayer.core.domain.locale.AppLanguage
import com.blueplayer.core.domain.locale.LanguageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class SharedPreferencesLanguageRepository(context: Context) : LanguageRepository {

    private companion object {
        const val PREFS_NAME = "blue_player_settings"
        const val KEY_LANGUAGE = "app_language"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _language = MutableStateFlow(loadLanguage())
    override val language: StateFlow<AppLanguage> = _language.asStateFlow()

    override fun setLanguage(lang: AppLanguage) {
        prefs.edit().putString(KEY_LANGUAGE, lang.code).apply()
        _language.value = lang
    }

    private fun loadLanguage(): AppLanguage {
        val saved = prefs.getString(KEY_LANGUAGE, null)
        if (saved != null) {
            AppLanguage.entries.firstOrNull { it.code == saved }?.let { return it }
        }

        // Если нет — берём язык системы, если он поддерживается
        val systemLang = Locale.getDefault().language
        return AppLanguage.entries.firstOrNull { it.code == systemLang }
            ?: AppLanguage.RUSSIAN
    }
}