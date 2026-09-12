package com.blueplayer.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.blueplayer.core.domain.locale.AppLanguage
import com.blueplayer.core.domain.locale.LanguageRepository
import com.blueplayer.core.domain.model.AccentColor
import com.blueplayer.core.domain.model.AppSettings
import com.blueplayer.core.domain.model.AudioFocusMode
import com.blueplayer.core.domain.model.SortOrder
import com.blueplayer.core.domain.model.ThemeMode
import com.blueplayer.core.domain.repository.SettingsRepository
import com.blueplayer.core.domain.repository.ThemeRepository
import com.blueplayer.core.domain.repository.TrackRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val language: AppLanguage = AppLanguage.RUSSIAN,
    val appSettings: AppSettings = AppSettings()
)

class SettingsViewModel(
    private val themeRepository: ThemeRepository,
    private val languageRepository: LanguageRepository,
    private val settingsRepository: SettingsRepository,
    private val trackRepository: TrackRepository
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
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _state.value = _state.value.copy(appSettings = settings)
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) = themeRepository.setThemeMode(mode)
    fun setLanguage(lang: AppLanguage) = languageRepository.setLanguage(lang)

    fun setDynamicColors(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.update { it.copy(dynamicColors = enabled) }
    }

    fun setAccentColor(color: AccentColor) = viewModelScope.launch {
        settingsRepository.update { it.copy(accentColor = color) }
    }

    fun setCrossfade(seconds: Int) = viewModelScope.launch {
        settingsRepository.update { it.copy(crossfadeSeconds = seconds) }
    }

    fun setAudioFocus(mode: AudioFocusMode) = viewModelScope.launch {
        settingsRepository.update { it.copy(audioFocusMode = mode) }
    }

    fun setKeepScreenOn(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.update { it.copy(keepScreenOn = enabled) }
    }

    fun setQueuePersistence(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.update { it.copy(queuePersistence = enabled) }
    }

    fun setShowWaveform(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.update { it.copy(showWaveform = enabled) }
    }

    fun setHapticFeedback(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.update { it.copy(hapticFeedback = enabled) }
    }

    fun setLoudnessNormalization(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.update { it.copy(loudnessNormalization = enabled) }
    }

    fun setMinTrackDuration(seconds: Int) = viewModelScope.launch {
        settingsRepository.update { it.copy(minTrackDurationSeconds = seconds) }
    }

    fun addExcludedFolder(folder: String) = viewModelScope.launch {
        val current = _state.value.appSettings.excludedFolders
        if (folder.isNotBlank() && folder !in current) {
            settingsRepository.update { it.copy(excludedFolders = current + folder) }
        }
    }

    fun removeExcludedFolder(folder: String) = viewModelScope.launch {
        settingsRepository.update { it.copy(excludedFolders = it.excludedFolders - folder) }
    }

    fun setAutoRescan(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.update { it.copy(autoRescanOnLaunch = enabled) }
    }

    fun setOnlineCovers(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.update { it.copy(onlineCoversEnabled = enabled) }
    }

    fun setDefaultSortOrder(order: SortOrder) = viewModelScope.launch {
        settingsRepository.update { it.copy(defaultSortOrder = order) }
    }

    fun rescanLibrary() = viewModelScope.launch {
        trackRepository.rescan()
    }

    fun resetAll() = viewModelScope.launch {
        settingsRepository.resetToDefaults()
    }
}

class SettingsViewModelFactory(
    private val themeRepository: ThemeRepository,
    private val languageRepository: LanguageRepository,
    private val settingsRepository: SettingsRepository,
    private val trackRepository: TrackRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        SettingsViewModel(themeRepository, languageRepository, settingsRepository, trackRepository) as T
}