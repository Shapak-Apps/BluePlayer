package com.blueplayer.core.player

import com.blueplayer.core.domain.model.AppSettings
import com.blueplayer.core.domain.player.PlayerController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

object CrossfadeMonitor {

    fun start(
        scope: CoroutineScope,
        controller: PlayerController,
        settings: StateFlow<AppSettings>
    ) {
        scope.launch(Dispatchers.Default) {
            var lastFactor = 1f
            while (isActive) {
                delay(120)
                val state = controller.state.value
                val crossfadeMs = settings.value.crossfadeSeconds * 1000L

                if (!state.isPlaying || state.durationMs <= 0 || crossfadeMs <= 0) {
                    if (lastFactor != 1f) {
                        controller.setVolume(VolumeHolder.base)
                        lastFactor = 1f
                    }
                    continue
                }

                val remaining = state.durationMs - state.positionMs
                val factor = when {
                    state.positionMs < crossfadeMs ->
                        (state.positionMs.toFloat() / crossfadeMs).coerceIn(0f, 1f)
                    remaining < crossfadeMs ->
                        (remaining.toFloat() / crossfadeMs).coerceIn(0f, 1f)
                    else -> 1f
                }

                if (abs(factor - lastFactor) > 0.02f || (factor == 1f && lastFactor != 1f)) {
                    controller.setVolume(VolumeHolder.base * factor)
                    lastFactor = factor
                }
            }
        }
    }
}