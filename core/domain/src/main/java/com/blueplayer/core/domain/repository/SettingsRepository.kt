package com.blueplayer.core.domain.repository

import com.blueplayer.core.domain.model.AppSettings
import kotlinx.coroutines.flow.StateFlow

interface SettingsRepository {
    val settings: StateFlow<AppSettings>
    suspend fun update(update: (AppSettings) -> AppSettings)
    suspend fun resetToDefaults()
}