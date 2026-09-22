package com.blueplayer.core.domain.changelog

import com.blueplayer.core.domain.locale.AppLanguage

data class ChangelogEntry(
    val version: String,
    val date: String,
    val changes: List<Change>
)

data class Change(
    val type: ChangeType,
    val description: (AppLanguage) -> String
)

enum class ChangeType {
    FEATURE,
    IMPROVEMENT,
    FIX
}

object Changelog {
    val entries = listOf(
        ChangelogEntry(
            version = "v1.0.0",
            date = "2026",
            changes = listOf(
                Change(ChangeType.FEATURE) { lang ->
                    when (lang) {
                        AppLanguage.RUSSIAN -> "Добавлен виджет на рабочий стол с управлением воспроизведением"
                        AppLanguage.ENGLISH -> "Added home screen widget with playback controls"
                        AppLanguage.TURKMEN -> "Esasy ekrana aýdym dolandyryş widjeti goşuldy"
                    }
                },
                Change(ChangeType.FEATURE) { lang ->
                    when (lang) {
                        AppLanguage.RUSSIAN -> "Экспорт и импорт настроек в файл"
                        AppLanguage.ENGLISH -> "Export and import settings to a file"
                        AppLanguage.TURKMEN -> "Sazlamalary faýla eksport we import"
                    }
                },
                Change(ChangeType.FEATURE) { lang ->
                    when (lang) {
                        AppLanguage.RUSSIAN -> "AMOLED тема для экономии батареи на OLED экранах"
                        AppLanguage.ENGLISH -> "AMOLED theme to save battery on OLED screens"
                        AppLanguage.TURKMEN -> "OLED ekranlarda batareýany tygşytlamak üçin AMOLED tema"
                    }
                },
                Change(ChangeType.FEATURE) { lang ->
                    when (lang) {
                        AppLanguage.RUSSIAN -> "Настройка размера текста (маленький, средний, большой)"
                        AppLanguage.ENGLISH -> "Text size adjustment (small, medium, large)"
                        AppLanguage.TURKMEN -> "Tekst ölçegini sazlamak (kiçi, orta, uly)"
                    }
                },
                Change(ChangeType.FEATURE) { lang ->
                    when (lang) {
                        AppLanguage.RUSSIAN -> "Выбор формы обложки: круглая, квадратная или скругленная"
                        AppLanguage.ENGLISH -> "Cover shape selection: circle, square or rounded"
                        AppLanguage.TURKMEN -> "Gabak görnüşini saýlamak: tegelek, kwadrat ýa-da tegelek burçly"
                    }
                },
                Change(ChangeType.FEATURE) { lang ->
                    when (lang) {
                        AppLanguage.RUSSIAN -> "Автоматическая загрузка обложек из интернета"
                        AppLanguage.ENGLISH -> "Automatic cover art download from the internet"
                        AppLanguage.TURKMEN -> "Internetden gabak suratyny awtomatiki ýüklemek"
                    }
                },
                Change(ChangeType.FEATURE) { lang ->
                    when (lang) {
                        AppLanguage.RUSSIAN -> "Удаление треков прямо из приложения"
                        AppLanguage.ENGLISH -> "Delete tracks directly from the app"
                        AppLanguage.TURKMEN -> "Aýdymlary göni programmadan pozmak"
                    }
                },
                Change(ChangeType.FEATURE) { lang ->
                    when (lang) {
                        AppLanguage.RUSSIAN -> "Эквалайзер для настройки звука"
                        AppLanguage.ENGLISH -> "Equalizer for sound customization"
                        AppLanguage.TURKMEN -> "Ses sazlamak üçin ekwalaýzer"
                    }
                },
                Change(ChangeType.FEATURE) { lang ->
                    when (lang) {
                        AppLanguage.RUSSIAN -> "Таймер сна: музыка остановится через указанное время"
                        AppLanguage.ENGLISH -> "Sleep timer: music stops after specified time"
                        AppLanguage.TURKMEN -> "Uky wagty: görkezilen wagtdan soň aýdym durýar"
                    }
                },
                Change(ChangeType.FEATURE) { lang ->
                    when (lang) {
                        AppLanguage.RUSSIAN -> "Закладки для быстрого возврата к моменту в треке"
                        AppLanguage.ENGLISH -> "Bookmarks for quick return to track moments"
                        AppLanguage.TURKMEN -> "Aýdym pursatlaryna çalt gaýdyp gelmek üçin bellikler"
                    }
                },
                Change(ChangeType.FEATURE) { lang ->
                    when (lang) {
                        AppLanguage.RUSSIAN -> "A-B повтор: зацикливание выбранного фрагмента"
                        AppLanguage.ENGLISH -> "A-B repeat: loop selected fragment"
                        AppLanguage.TURKMEN -> "A-B gaýtalama: saýlanan bölegi gaýtalama"
                    }
                },
                Change(ChangeType.FEATURE) { lang ->
                    when (lang) {
                        AppLanguage.RUSSIAN -> "Плавный переход между треками (кроссфейд)"
                        AppLanguage.ENGLISH -> "Smooth transition between tracks (crossfade)"
                        AppLanguage.TURKMEN -> "Aýdymlaryň arasynda ýumşak geçiş (crossfade)"
                    }
                },
                Change(ChangeType.FEATURE) { lang ->
                    when (lang) {
                        AppLanguage.RUSSIAN -> "Сохранение очереди воспроизведения"
                        AppLanguage.ENGLISH -> "Playback queue persistence"
                        AppLanguage.TURKMEN -> "Aýdym nobatyny saklamak"
                    }
                },
                Change(ChangeType.FEATURE) { lang ->
                    when (lang) {
                        AppLanguage.RUSSIAN -> "Создание и управление плейлистами"
                        AppLanguage.ENGLISH -> "Create and manage playlists"
                        AppLanguage.TURKMEN -> "Aýdym sanawlaryny döretmek we dolandyrmak"
                    }
                },
                Change(ChangeType.FEATURE) { lang ->
                    when (lang) {
                        AppLanguage.RUSSIAN -> "Избранные треки для быстрого доступа"
                        AppLanguage.ENGLISH -> "Favorite tracks for quick access"
                        AppLanguage.TURKMEN -> "Çalt elýeterlilik üçin halanýan aýdymlar"
                    }
                },
                Change(ChangeType.IMPROVEMENT) { lang ->
                    when (lang) {
                        AppLanguage.RUSSIAN -> "Быстрая навигация по папкам, альбомам и исполнителям"
                        AppLanguage.ENGLISH -> "Fast navigation through folders, albums and artists"
                        AppLanguage.TURKMEN -> "Bukjalar, albomlar we aýdymçylar boýunça çalt nawigasiýa"
                    }
                },
                Change(ChangeType.IMPROVEMENT) { lang ->
                    when (lang) {
                        AppLanguage.RUSSIAN -> "Поддержка русского, английского и туркменского языков"
                        AppLanguage.ENGLISH -> "Russian, English and Turkmen language support"
                        AppLanguage.TURKMEN -> "Rus, iňlis we türkmen dil goldawy"
                    }
                },
                Change(ChangeType.IMPROVEMENT) { lang ->
                    when (lang) {
                        AppLanguage.RUSSIAN -> "Современный дизайн Material 3"
                        AppLanguage.ENGLISH -> "Modern Material 3 design"
                        AppLanguage.TURKMEN -> "Häzirki zaman Material 3 dizaýny"
                    }
                }
            )
        )
    )
}