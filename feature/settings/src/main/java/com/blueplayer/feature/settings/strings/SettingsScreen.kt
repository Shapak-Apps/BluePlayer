package com.blueplayer.feature.settings

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.blueplayer.core.domain.locale.AppLanguage
import com.blueplayer.core.domain.locale.Strings
import com.blueplayer.core.domain.model.AccentColor
import com.blueplayer.core.domain.model.AudioFocusMode
import com.blueplayer.core.domain.model.CoverShape
import com.blueplayer.core.domain.model.SortOrder
import com.blueplayer.core.domain.model.TextSize
import com.blueplayer.core.domain.model.ThemeMode
import com.blueplayer.core.domain.repository.SettingsRepository
import com.blueplayer.core.player.CoverCache
import com.blueplayer.core.player.OnlineCoverFetcher

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModelFactory: ViewModelProvider.Factory,
    settingsRepository: SettingsRepository,
    coverCache: CoverCache,
    onBack: () -> Unit,
    onGithubClick: () -> Unit,
    onEqualizerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: SettingsViewModel = viewModel(factory = viewModelFactory)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lang = state.language
    val context = LocalContext.current

    var expandedInterface by remember { mutableStateOf(true) }
    var expandedSound by remember { mutableStateOf(false) }
    var expandedPlayback by remember { mutableStateOf(false) }
    var expandedLibrary by remember { mutableStateOf(false) }
    var expandedAdvanced by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showAddFolderDialog by remember { mutableStateOf(false) }
    var folderNameInput by remember { mutableStateOf("") }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            TopAppBar(
                title = { Text(Strings.settings(lang)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )

            Column(Modifier.padding(16.dp)) {

                SettingsCard(
                    icon = Icons.Filled.Palette,
                    title = Strings.settingsInterface(lang),
                    subtitle = Strings.settingsInterfaceDesc(lang),
                    expanded = expandedInterface,
                    onToggle = { expandedInterface = !expandedInterface }
                ) {
                    Text(Strings.theme(lang), style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = state.themeMode == ThemeMode.SYSTEM,
                            onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) },
                            label = { Text(Strings.themeSystem(lang)) }
                        )
                        FilterChip(
                            selected = state.themeMode == ThemeMode.LIGHT,
                            onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) },
                            label = { Text(Strings.themeLight(lang)) }
                        )
                        FilterChip(
                            selected = state.themeMode == ThemeMode.DARK,
                            onClick = { viewModel.setThemeMode(ThemeMode.DARK) },
                            label = { Text(Strings.themeDark(lang)) }
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    Text(Strings.language(lang), style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppLanguage.entries.forEach { language ->
                            FilterChip(
                                selected = state.language == language,
                                onClick = { viewModel.setLanguage(language) },
                                label = { Text(language.displayName) }
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            Strings.dynamicColors(lang),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = state.appSettings.dynamicColors,
                            onCheckedChange = { viewModel.setDynamicColors(it) }
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    Text(Strings.accentColor(lang), style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    AccentColorPicker(
                        selected = state.appSettings.accentColor,
                        onSelect = { viewModel.setAccentColor(it) }
                    )

                    Spacer(Modifier.height(20.dp))
                    Text(Strings.textSize(lang), style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextSize.entries.forEach { size ->
                            FilterChip(
                                selected = state.appSettings.textSize == size,
                                onClick = { viewModel.setTextSize(size) },
                                label = { Text(Strings.textSizeName(lang, size)) }
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Text(Strings.coverShape(lang), style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CoverShape.entries.forEach { shape ->
                            FilterChip(
                                selected = state.appSettings.coverShape == shape,
                                onClick = { viewModel.setCoverShape(shape) },
                                label = { Text(Strings.coverShapeName(lang, shape)) }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                SettingsCard(
                    icon = Icons.Filled.VolumeUp,
                    title = Strings.settingsSound(lang),
                    subtitle = Strings.settingsSoundDesc(lang),
                    expanded = expandedSound,
                    onToggle = { expandedSound = !expandedSound }
                ) {
                    OutlinedButton(
                        onClick = onEqualizerClick,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(Strings.equalizer(lang))
                    }

                    Spacer(Modifier.height(12.dp))
                    SettingToggle(
                        title = Strings.normalize(lang),
                        checked = state.appSettings.loudnessNormalization,
                        onChange = { viewModel.setLoudnessNormalization(it) }
                    )
                }

                Spacer(Modifier.height(12.dp))

                SettingsCard(
                    icon = Icons.Filled.PlayCircle,
                    title = Strings.settingsPlayback(lang),
                    subtitle = Strings.settingsPlaybackDesc(lang),
                    expanded = expandedPlayback,
                    onToggle = { expandedPlayback = !expandedPlayback }
                ) {
                    Text(Strings.crossfade(lang), style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (state.appSettings.crossfadeSeconds == 0)
                            Strings.crossfadeOff(lang)
                        else
                            Strings.crossfadeSeconds(lang, state.appSettings.crossfadeSeconds),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Slider(
                        value = state.appSettings.crossfadeSeconds.toFloat(),
                        onValueChange = { viewModel.setCrossfade(it.toInt()) },
                        valueRange = 0f..12f,
                        steps = 11
                    )

                    Spacer(Modifier.height(20.dp))
                    Text(Strings.audioFocus(lang), style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = state.appSettings.audioFocusMode == AudioFocusMode.PAUSE,
                            onClick = { viewModel.setAudioFocus(AudioFocusMode.PAUSE) },
                            label = { Text(Strings.audioFocusPause(lang)) }
                        )
                        FilterChip(
                            selected = state.appSettings.audioFocusMode == AudioFocusMode.DUCK,
                            onClick = { viewModel.setAudioFocus(AudioFocusMode.DUCK) },
                            label = { Text(Strings.audioFocusDuck(lang)) }
                        )
                        FilterChip(
                            selected = state.appSettings.audioFocusMode == AudioFocusMode.IGNORE,
                            onClick = { viewModel.setAudioFocus(AudioFocusMode.IGNORE) },
                            label = { Text(Strings.audioFocusIgnore(lang)) }
                        )
                    }

                    Spacer(Modifier.height(20.dp))
                    SettingToggle(
                        title = Strings.keepScreenOn(lang),
                        checked = state.appSettings.keepScreenOn,
                        onChange = { viewModel.setKeepScreenOn(it) }
                    )

                    Spacer(Modifier.height(12.dp))
                    SettingToggle(
                        title = Strings.queuePersistence(lang),
                        checked = state.appSettings.queuePersistence,
                        onChange = { viewModel.setQueuePersistence(it) }
                    )

                    Spacer(Modifier.height(12.dp))
                    SettingToggle(
                        title = Strings.showWaveform(lang),
                        checked = state.appSettings.showWaveform,
                        onChange = { viewModel.setShowWaveform(it) }
                    )

                    Spacer(Modifier.height(12.dp))
                    SettingToggle(
                        title = Strings.hapticFeedback(lang),
                        checked = state.appSettings.hapticFeedback,
                        onChange = { viewModel.setHapticFeedback(it) }
                    )
                }

                Spacer(Modifier.height(12.dp))

                SettingsCard(
                    icon = Icons.Filled.LibraryMusic,
                    title = Strings.settingsLibrary(lang),
                    subtitle = Strings.settingsLibraryDesc(lang),
                    expanded = expandedLibrary,
                    onToggle = { expandedLibrary = !expandedLibrary }
                ) {
                    Text(Strings.minTrackDuration(lang), style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        Strings.seconds(lang, state.appSettings.minTrackDurationSeconds),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Slider(
                        value = state.appSettings.minTrackDurationSeconds.toFloat(),
                        onValueChange = { viewModel.setMinTrackDuration(it.toInt()) },
                        valueRange = 0f..120f,
                        steps = 23
                    )
                    Text(
                        Strings.minTrackDurationDesc(lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(20.dp))
                    Text(Strings.excludedFolders(lang), style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        Strings.excludedFoldersDesc(lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))

                    state.appSettings.excludedFolders.forEach { folder ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    folder,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { viewModel.removeExcludedFolder(folder) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Close,
                                        null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            folderNameInput = ""
                            showAddFolderDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Add, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(Strings.addFolder(lang))
                    }

                    Spacer(Modifier.height(20.dp))
                    SettingToggle(
                        title = Strings.autoRescan(lang),
                        checked = state.appSettings.autoRescanOnLaunch,
                        onChange = { viewModel.setAutoRescan(it) }
                    )
                    Text(
                        Strings.autoRescanDesc(lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    Spacer(Modifier.height(12.dp))
                    SettingToggle(
                        title = Strings.onlineCovers(lang),
                        checked = state.appSettings.onlineCoversEnabled,
                        onChange = { viewModel.setOnlineCovers(it) }
                    )
                    Text(
                        Strings.onlineCoversDesc(lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    Spacer(Modifier.height(16.dp))
                    Text(Strings.defaultSort(lang), style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    SortOrder.entries.forEach { order ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.setDefaultSortOrder(order) }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = state.appSettings.defaultSortOrder == order,
                                onClick = { viewModel.setDefaultSortOrder(order) }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                Strings.sortOrderName(lang, order),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                SettingsCard(
                    icon = Icons.Filled.SettingsSuggest,
                    title = Strings.settingsAdvanced(lang),
                    subtitle = Strings.settingsAdvancedDesc(lang),
                    expanded = expandedAdvanced,
                    onToggle = { expandedAdvanced = !expandedAdvanced }
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.rescanLibrary()
                            Toast.makeText(context, Strings.libraryRescanned(lang), Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(Strings.rescanLibrary(lang))
                    }

                    Spacer(Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            val cacheDir = context.cacheDir
                            val imageCache = java.io.File(cacheDir, "image_cache")
                            if (imageCache.exists()) imageCache.deleteRecursively()
                            Toast.makeText(context, Strings.cacheCleared(lang), Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Filled.CleaningServices, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(Strings.clearImageCache(lang))
                    }

                    Spacer(Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            val fetcher = OnlineCoverFetcher(context, coverCache)
                            fetcher.clearCache()
                            Toast.makeText(context, Strings.coverCacheCleared(lang), Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Filled.CleaningServices, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(Strings.clearCoverCache(lang))
                    }

                    Spacer(Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Filled.Restore, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(Strings.resetSettings(lang))
                    }
                }
            }
        }
    }

    if (showAddFolderDialog) {
        AlertDialog(
            onDismissRequest = { showAddFolderDialog = false },
            title = { Text(Strings.addFolder(lang)) },
            text = {
                OutlinedTextField(
                    value = folderNameInput,
                    onValueChange = { folderNameInput = it },
                    label = { Text(Strings.folderName(lang)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    enabled = folderNameInput.isNotBlank(),
                    onClick = {
                        viewModel.addExcludedFolder(folderNameInput.trim())
                        showAddFolderDialog = false
                    }
                ) { Text(Strings.add(lang)) }
            },
            dismissButton = {
                TextButton(onClick = { showAddFolderDialog = false }) {
                    Text(Strings.cancel(lang))
                }
            }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            icon = { Icon(Icons.Filled.Restore, null) },
            title = { Text(Strings.resetSettingsConfirmTitle(lang)) },
            text = { Text(Strings.resetSettingsConfirmText(lang)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resetAll()
                    showResetDialog = false
                    Toast.makeText(context, Strings.settingsReset(lang), Toast.LENGTH_SHORT).show()
                }) {
                    Text(Strings.resetSettings(lang), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(Strings.cancel(lang))
                }
            }
        )
    }
}

@Composable
private fun AccentColorPicker(
    selected: AccentColor,
    onSelect: (AccentColor) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AccentColor.entries.chunked(4).forEach { rowColors ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                rowColors.forEach { color ->
                    val isSelected = selected == color
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(color.argb))
                            .then(
                                if (isSelected) Modifier.border(
                                    width = 3.dp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = CircleShape
                                ) else Modifier
                            )
                            .clickable { onSelect(color) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingToggle(
    title: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun SettingsCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            icon,
                            null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    Icons.Filled.ArrowDropDown,
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(Modifier.height(16.dp))
                    content()
                }
            }
        }
    }
}