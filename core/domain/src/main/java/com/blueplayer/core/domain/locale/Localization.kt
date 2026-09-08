package com.blueplayer.feature.settings.strings

enum class AppLanguage(val code: String, val displayName: String) {
    RUSSIAN("ru", "Русский"),
    ENGLISH("en", "English"),
    TURKMEN("tk", "Türkmen")
}

object Strings {
    fun tracks(language: AppLanguage): String = when (language) {
        AppLanguage.RUSSIAN -> "Треки"
        AppLanguage.ENGLISH -> "Tracks"
        AppLanguage.TURKMEN -> "Aýdymlar"
    }

    fun albums(language: AppLanguage): String = when (language) {
        AppLanguage.RUSSIAN -> "Альбомы"
        AppLanguage.ENGLISH -> "Albums"
        AppLanguage.TURKMEN -> "Albomlar"
    }

    fun artists(language: AppLanguage): String = when (language) {
        AppLanguage.RUSSIAN -> "Исполнители"
        AppLanguage.ENGLISH -> "Artists"
        AppLanguage.TURKMEN -> "Ýerine ýetirijiler"
    }

    fun folders(language: AppLanguage): String = when (language) {
        AppLanguage.RUSSIAN -> "Папки"
        AppLanguage.ENGLISH -> "Folders"
        AppLanguage.TURKMEN -> "Papka"
    }

    fun settings(language: AppLanguage): String = when (language) {
        AppLanguage.RUSSIAN -> "Настройки"
        AppLanguage.ENGLISH -> "Settings"
        AppLanguage.TURKMEN -> "Sazlamalar"
    }

    fun theme(language: AppLanguage): String = when (language) {
        AppLanguage.RUSSIAN -> "Тема"
        AppLanguage.ENGLISH -> "Theme"
        AppLanguage.TURKMEN -> "Tema"
    }

    fun themeSystem(language: AppLanguage): String = when (language) {
        AppLanguage.RUSSIAN -> "Системная"
        AppLanguage.ENGLISH -> "System"
        AppLanguage.TURKMEN -> "Ulgam"
    }

    fun themeLight(language: AppLanguage): String = when (language) {
        AppLanguage.RUSSIAN -> "Светлая"
        AppLanguage.ENGLISH -> "Light"
        AppLanguage.TURKMEN -> "Ýagty"
    }

    fun themeDark(language: AppLanguage): String = when (language) {
        AppLanguage.RUSSIAN -> "Тёмная"
        AppLanguage.ENGLISH -> "Dark"
        AppLanguage.TURKMEN -> "Garaňky"
    }

    fun language(language: AppLanguage): String = when (language) {
        AppLanguage.RUSSIAN -> "Язык"
        AppLanguage.ENGLISH -> "Language"
        AppLanguage.TURKMEN -> "Dil"
    }

    fun about(language: AppLanguage): String = when (language) {
        AppLanguage.RUSSIAN -> "О приложении"
        AppLanguage.ENGLISH -> "About"
        AppLanguage.TURKMEN -> "Programma barada"
    }

    fun version(language: AppLanguage): String = when (language) {
        AppLanguage.RUSSIAN -> "Версия"
        AppLanguage.ENGLISH -> "Version"
        AppLanguage.TURKMEN -> "Wersiýa"
    }

    fun developer(language: AppLanguage): String = when (language) {
        AppLanguage.RUSSIAN -> "Разработчик"
        AppLanguage.ENGLISH -> "Developer"
        AppLanguage.TURKMEN -> "Düzediji"
    }

    fun github(language: AppLanguage): String = when (language) {
        AppLanguage.RUSSIAN -> "GitHub"
        AppLanguage.ENGLISH -> "GitHub"
        AppLanguage.TURKMEN -> "GitHub"
    }
}