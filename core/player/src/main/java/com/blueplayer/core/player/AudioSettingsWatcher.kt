package com.blueplayer.core.player

import com.blueplayer.core.domain.model.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AudioSettingsWatcher(
    private val engine: EqualizerEngine
) {

    fun start(scope: CoroutineScope, settings: StateFlow<AppSettings>) {
        scope.launch {
            settings.collect { s ->
                applyLoudness(s.loudnessNormalization)
            }
        }
    }

    private fun applyLoudness(enabled: Boolean) {
        val names = listOf(
            "setLoudnessNormalization",
            "setLoudness",
            "setNormalization",
            "setLoudnessEnabled"
        )
        runCatching {
            val method = engine.javaClass.methods.firstOrNull { m ->
                m.name in names && m.parameterTypes.size == 1 &&
                        m.parameterTypes[0] == Boolean::class.javaPrimitiveType
            }
            method?.invoke(engine, enabled)
        }
    }
}