package com.blueplayer.core.domain.player

enum class RepeatModeUi { OFF, ALL, ONE }

data class PlaybackOptions(
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatModeUi = RepeatModeUi.OFF,
    val sleepTimerActive: Boolean = false,
    val abState: Int = 0
)