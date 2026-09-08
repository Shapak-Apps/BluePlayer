package com.blueplayer.core.domain.locale

import kotlinx.coroutines.flow.StateFlow

interface LanguageRepository {
    val language: StateFlow<AppLanguage>
    fun setLanguage(lang: AppLanguage)
}