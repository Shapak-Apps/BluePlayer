package com.blueplayer.feature.nowplaying

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import kotlin.random.Random

fun generatePseudoWave(seed: Long, count: Int): FloatArray {
    val random = Random(seed)
    return FloatArray(count) { 0.2f + random.nextFloat() * 0.8f }
}

@Composable
fun WaveformSeekBar(
    progress: Float,
    waveform: FloatArray,
    seed: Long,
    playedColor: Color,
    unplayedColor: Color,
    currentPositionMs: Long,
    durationMs: Long,
    onSeek: (Float) -> Unit,
    hapticEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val bars = remember(waveform, seed) {
        if (waveform.isNotEmpty()) waveform else generatePseudoWave(seed, 120)
    }
    val barCount = bars.size

    var drag by remember { mutableStateOf<Float?>(null) }
    var lastSeekTime by remember { mutableLongStateOf(0L) }

    val display = drag ?: progress

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(horizontal = 8.dp)
                .pointerInput(durationMs, seed) {
                    forEachGesture {
                        awaitPointerEventScope {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()

                            val f = (down.position.x / size.width).coerceIn(0f, 1f)
                            drag = f
                            onSeek(f)
                            lastSeekTime = System.currentTimeMillis()
                            if (hapticEnabled) {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            }

                            var pressed = true
                            var currentF = f
                            while (pressed) {
                                val event = awaitPointerEvent()
                                pressed = event.changes.any { it.pressed }
                                if (pressed) {
                                    val change = event.changes.first()
                                    change.consume()
                                    currentF = (change.position.x / size.width).coerceIn(0f, 1f)
                                    drag = currentF

                                    val now = System.currentTimeMillis()
                                    if (now - lastSeekTime > 50L) {
                                        onSeek(currentF)
                                        lastSeekTime = now
                                    }
                                }
                            }

                            drag = null
                            onSeek(currentF)
                        }
                    }
                }
        ) {
            val barWidth = size.width / barCount
            val gap = barWidth * 0.3f

            bars.forEachIndexed { i, amplitude ->
                val h = (0.06f + amplitude * 0.94f) * size.height
                val x = i * barWidth
                val y = (size.height - h) / 2f
                val played = i.toFloat() / barCount <= display

                drawRoundRect(
                    color = if (played) playedColor else unplayedColor,
                    topLeft = Offset(x, y),
                    size = Size(barWidth - gap, h),
                    cornerRadius = CornerRadius(2f, 2f)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val displayPosition = if (drag != null) {
                (drag!! * durationMs).toLong()
            } else {
                currentPositionMs
            }

            Text(
                formatDuration(displayPosition),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                formatDuration(durationMs),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SimpleSeekBar(
    progress: Float,
    currentPositionMs: Long,
    durationMs: Long,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Slider(
            value = progress.coerceIn(0f, 1f),
            onValueChange = { onSeek(it) },
            valueRange = 0f..1f,
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                formatDuration(currentPositionMs),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                formatDuration(durationMs),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}