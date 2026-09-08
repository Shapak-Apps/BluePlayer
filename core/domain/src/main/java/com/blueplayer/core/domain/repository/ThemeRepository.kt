package com.blueplayer.core.domain.repository

import com.blueplayer.core.domain.model.ThemeMode
import kotlinx.coroutines.flow.StateFlow

interface ThemeRepository {
    val themeMode: StateFlow<ThemeMode>
    fun setThemeMode(mode: ThemeMode)
}