package com.blueplayer.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.blueplayer.core.domain.locale.AppLanguage
import com.blueplayer.core.domain.locale.LanguageRepository
import com.blueplayer.core.domain.model.ThemeMode
import com.blueplayer.core.domain.repository.ThemeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val language: AppLanguage = AppLanguage.RUSSIAN
)

class SettingsViewModel(
    private val themeRepository: ThemeRepository,
    private val languageRepository: LanguageRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            themeRepository.themeMode.collect { mode ->
                _state.value = _state.value.copy(themeMode = mode)
            }
        }
        viewModelScope.launch {
            languageRepository.language.collect { lang ->
                _state.value = _state.value.copy(language = lang)
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) = themeRepository.setThemeMode(mode)
    fun setLanguage(lang: AppLanguage) = languageRepository.setLanguage(lang)
}

class SettingsViewModelFactory(
    private val themeRepository: ThemeRepository,
    private val languageRepository: LanguageRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        SettingsViewModel(themeRepository, languageRepository) as T
}