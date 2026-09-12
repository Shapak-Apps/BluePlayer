package com.blueplayer.feature.settings.equalizer

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.media.audiofx.PresetReverb
import com.blueplayer.core.domain.locale.AppLanguage
import com.blueplayer.core.domain.locale.Strings
import com.blueplayer.core.player.BandInfo
import com.blueplayer.core.player.EqualizerEngine
import com.blueplayer.core.player.VolumeHolder
import com.blueplayer.core.domain.player.PlayerController
import kotlin.math.pow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(
    engine: EqualizerEngine,
    sessionId: Int?,
    playerController: PlayerController,
    lang: AppLanguage,
    onBack: () -> Unit
) {
    var available by remember { mutableStateOf(false) }
    var enabled by remember { mutableStateOf(engine.isEnabled) }
    var bands by remember { mutableStateOf<List<BandInfo>>(emptyList()) }
    var preGain by remember { mutableFloatStateOf(engine.preGainDb) }
    var bassBoost by remember { mutableFloatStateOf(engine.bassBoostPercent) }
    var virtualizer by remember { mutableFloatStateOf(engine.virtStrength) }
    var preset by remember { mutableStateOf("Flat") }
    var showPresetsSheet by remember { mutableStateOf(false) }
    var showReverbSheet by remember { mutableStateOf(false) }

    LaunchedEffect(sessionId) {
        if (sessionId != null && sessionId != 0) {
            engine.attach(sessionId)
            available = engine.isAvailable
            enabled = engine.isEnabled
            bands = engine.readBands()
            preGain = engine.preGainDb
            bassBoost = engine.bassBoostPercent
            virtualizer = engine.virtStrength
        }
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                    )
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.GraphicEq, null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            Strings.equalizer(lang),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            preset,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, null)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
            )
        )

        if (!available) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.Tune, null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        Strings.eqNotAvailable(lang),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(Modifier.height(16.dp))

                Text(
                    "BASS BOOSTER",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(Modifier.height(8.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.Bolt, null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Bass",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                "${bassBoost.toInt()}%",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Slider(
                            value = bassBoost,
                            onValueChange = { v ->
                                bassBoost = v
                                engine.setBassBoost(v)
                                preset = "Custom"
                            },
                            valueRange = 0f..100f,
                            enabled = enabled,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("0%", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("MAX 100%", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold)
                        }
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("0%", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("MAX 200%", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    "EQUALIZER",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(Modifier.height(8.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    Column(Modifier.padding(vertical = 20.dp, horizontal = 12.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Pre-amp: ${"%.1f".format(preGain)} dB",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                if (enabled) "ON" else "OFF",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (enabled) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = preGain,
                            onValueChange = { v ->
                                preGain = v
                                engine.setPreGain(v)
                                val vol = if (v < 0) 10f.pow(v / 20f) else 1f
                                VolumeHolder.base = vol
                                playerController.setVolume(vol)
                            },
                            valueRange = -20f..20f,
                            enabled = enabled,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(20.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            bands.forEach { band ->
                                VerticalBandSlider(
                                    band = band,
                                    enabled = enabled,
                                    onValueChange = { v ->
                                        bands = bands.map {
                                            if (it.index == band.index) it.copy(level = v) else it
                                        }
                                        engine.setBandLevel(band.index, v)
                                        preset = "Custom"
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    "EFFECTS",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(Modifier.height(8.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.SurroundSound, null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Virtualizer",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                "${virtualizer.toInt()}%",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Slider(
                            value = virtualizer,
                            onValueChange = { v ->
                                virtualizer = v
                                engine.setVirtualizer(v)
                            },
                            valueRange = 0f..1000f,
                            enabled = enabled,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(12.dp))

                        TextButton(
                            onClick = { showReverbSheet = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Reverb settings", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            enabled = false
                            engine.setEnabled(false)
                        }
                    ) {
                        Text(
                            Strings.off(lang),
                            color = if (!enabled) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                        )
                    }
                    TextButton(
                        onClick = {
                            enabled = true
                            engine.setEnabled(true)
                        }
                    ) {
                        Text(
                            Strings.auto(lang),
                            color = if (enabled) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { showPresetsSheet = true }) {
                        Text(
                            "PRESETS",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    if (showPresetsSheet) {
        ModalBottomSheet(onDismissRequest = { showPresetsSheet = false }) {
            Column(Modifier.padding(bottom = 32.dp)) {
                Text(
                    Strings.eqPresets(lang),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
                LazyColumn {
                    items(EqualizerEngine.PRESET_CURVES.keys.toList()) { name ->
                        val isSelected = name == preset
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .clickable {
                                    preset = name
                                    bands = engine.applyPreset(name)
                                    showPresetsSheet = false
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(
                                                MaterialTheme.colorScheme.primary,
                                                CircleShape
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showReverbSheet) {
        val reverbPresets = listOf(
            "None" to PresetReverb.PRESET_NONE,
            "Small Room" to PresetReverb.PRESET_SMALLROOM,
            "Medium Room" to PresetReverb.PRESET_MEDIUMROOM,
            "Large Room" to PresetReverb.PRESET_LARGEROOM,
            "Medium Hall" to PresetReverb.PRESET_MEDIUMHALL,
            "Large Hall" to PresetReverb.PRESET_LARGEHALL,
            "Plate" to PresetReverb.PRESET_PLATE
        )

        ModalBottomSheet(onDismissRequest = { showReverbSheet = false }) {
            Column(Modifier.padding(bottom = 32.dp)) {
                Text(
                    "Reverb",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
                LazyColumn {
                    items(reverbPresets) { (name, presetValue) ->
                        val isSelected = engine.reverbPreset == presetValue
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .clickable {
                                    engine.setReverb(presetValue)
                                    showReverbSheet = false
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(
                                                MaterialTheme.colorScheme.primary,
                                                CircleShape
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VerticalBandSlider(
    band: BandInfo,
    enabled: Boolean,
    onValueChange: (Float) -> Unit
) {
    var currentValue by remember(band.index) { mutableFloatStateOf(band.level) }

    LaunchedEffect(band.level) {
        currentValue = band.level
    }

    val progress = ((currentValue - band.min) / (band.max - band.min)).coerceIn(0f, 1f)
    val thumbColor by animateColorAsState(
        targetValue = if (currentValue > 0f) MaterialTheme.colorScheme.primary
        else if (currentValue < 0f) MaterialTheme.colorScheme.tertiary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200),
        label = "thumb"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(36.dp)
    ) {
        Text(
            "${(currentValue / 100f).toInt()}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight(progress)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                thumbColor.copy(alpha = 0.3f),
                                thumbColor
                            )
                        )
                    )
                    .align(Alignment.BottomCenter)
            )

            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(thumbColor, CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    .align(Alignment.BottomCenter)
                    .padding(bottom = ((1f - progress) * 180).dp)
                    .then(
                        if (enabled) Modifier.pointerInput(band.index) {
                            detectVerticalDragGestures(
                                onDragEnd = {},
                                onDragCancel = {},
                                onDragStart = {},
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    val delta = -dragAmount / size.height * (band.max - band.min) * 2
                                    val newValue = (currentValue + delta).coerceIn(band.min, band.max)
                                    currentValue = newValue
                                    onValueChange(newValue)
                                }
                            )
                        } else Modifier
                    )
            )
        }

        Text(
            band.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 9.sp,
            maxLines = 1
        )
    }
}