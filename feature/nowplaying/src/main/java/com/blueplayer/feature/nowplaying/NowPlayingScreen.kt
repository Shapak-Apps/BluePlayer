package com.blueplayer.feature.nowplaying

import android.content.Intent
import android.net.Uri
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.blueplayer.core.domain.locale.AppLanguage
import com.blueplayer.core.domain.locale.Strings
import com.blueplayer.core.domain.player.PlaybackOptions
import com.blueplayer.core.domain.player.PlayerController
import com.blueplayer.core.domain.player.PlayerState
import com.blueplayer.core.domain.player.RepeatModeUi
import com.blueplayer.core.domain.repository.BookmarksRepository
import com.blueplayer.core.domain.repository.FavoritesRepository
import com.blueplayer.core.domain.repository.PlaylistsRepository
import com.blueplayer.core.player.OnlineCoverFetcher
import com.blueplayer.core.player.WaveformExtractor
import com.blueplayer.ui.components.ArtworkPlaceholder
import com.blueplayer.ui.components.GlideArtwork
import kotlinx.coroutines.launch

private val SPEEDS = listOf(0.75f, 1f, 1.25f, 1.5f, 2f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    state: PlayerState,
    options: PlaybackOptions,
    playerController: PlayerController,
    favoritesRepository: FavoritesRepository,
    playlistsRepository: PlaylistsRepository,
    bookmarksRepository: BookmarksRepository,
    lang: AppLanguage,
    onOpenDrawer: () -> Unit,
    onEqualizerClick: () -> Unit,
    onAlbumClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val track = state.currentTrack
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val favorites by favoritesRepository.favorites.collectAsStateWithLifecycle()
    val playlists by playlistsRepository.playlists.collectAsStateWithLifecycle()
    val bookmarks by bookmarksRepository.bookmarks.collectAsStateWithLifecycle()

    val coverFetcher = remember { OnlineCoverFetcher(context) }
    var coverModel by remember { mutableStateOf<Any?>(null) }

    var waveform by remember { mutableStateOf(FloatArray(0)) }
    LaunchedEffect(track?.uri) {
        if (track != null) {
            val uri = runCatching { Uri.parse(track.uri) }.getOrNull()
            waveform = if (uri != null) {
                WaveformExtractor.extract(context, uri)
            } else {
                FloatArray(0)
            }
        }
    }

    LaunchedEffect(track?.id) {
        val t = track ?: return@LaunchedEffect
        coverModel = null
        if (!t.artworkUri.isNullOrBlank()) {
            coverModel = t.artworkUri
        } else {
            val url = coverFetcher.getCoverUrl(t.id, t.title, t.artist)
            if (url != null) coverModel = url
        }
    }

    var showSleepSheet by remember { mutableStateOf(false) }
    var showPlaylistSheet by remember { mutableStateOf(false) }
    var showBookmarksSheet by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    if (track == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(Strings.nothingPlaying(lang), style = MaterialTheme.typography.titleLarge)
        }
        return
    }

    val isFavorite = favorites.any { it.id == track.id }
    val seed = track.id.toLongOrNull() ?: track.title.hashCode().toLong()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onOpenDrawer) {
                Icon(Icons.Filled.Menu, null)
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { scope.launch { favoritesRepository.toggleFavorite(track) } }) {
                Icon(
                    if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    null,
                    tint = if (isFavorite) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
            ) {
                IconButton(onClick = onEqualizerClick) {
                    Icon(Icons.Filled.Tune, null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            IconButton(onClick = { showSleepSheet = true }) {
                Icon(
                    if (options.sleepTimerActive) Icons.Filled.TimerOff else Icons.Filled.Timer,
                    null,
                    tint = if (options.sleepTimerActive) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(1f, matchHeightConstraintsFirst = true)
                        .shadow(16.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        ArtworkPlaceholder(Modifier.fillMaxSize())
                        if (coverModel != null) {
                            GlideArtwork(
                                model = coverModel,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.clickable { showInfoDialog = true }
                ) {
                    Text(
                        "INFO",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { showBookmarksSheet = true }) {
                    Icon(Icons.Filled.BookmarkBorder, null)
                }
                Text(
                    Strings.noLyrics(lang),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Filled.MoreVert, null)
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(Strings.info(lang)) },
                            onClick = { menuExpanded = false; showInfoDialog = true }
                        )
                        DropdownMenuItem(
                            text = { Text(Strings.addToFavorites(lang)) },
                            onClick = {
                                menuExpanded = false
                                scope.launch { favoritesRepository.toggleFavorite(track) }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(Strings.goToAlbum(lang)) },
                            onClick = { menuExpanded = false; onAlbumClick(track.albumId) }
                        )
                        DropdownMenuItem(
                            text = { Text(Strings.share(lang)) },
                            onClick = {
                                menuExpanded = false
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "audio/*"
                                    putExtra(Intent.EXTRA_STREAM, Uri.parse(track.uri))
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(
                                    Intent.createChooser(intent, Strings.share(lang))
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(Strings.sendToPlaylists(lang)) },
                            onClick = { menuExpanded = false; showPlaylistSheet = true }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            WaveformSeekBarWithGestures(
                progress = if (state.durationMs > 0)
                    state.positionMs.toFloat() / state.durationMs.toFloat() else 0f,
                waveform = waveform,
                seed = seed,
                playedColor = MaterialTheme.colorScheme.primary,
                unplayedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                currentPositionMs = state.positionMs,
                durationMs = state.durationMs,
                onSeek = { f -> playerController.seekTo((f * state.durationMs).toLong()) }
            )

            Spacer(Modifier.height(12.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .padding(vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { playerController.toggleShuffle() }) {
                    Icon(Icons.Filled.Shuffle, null,
                        tint = if (options.shuffleEnabled) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f))
                }
                IconButton(onClick = { playerController.previous() }) {
                    Icon(Icons.Filled.SkipPrevious, null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(36.dp))
                }
                Surface(
                    modifier = Modifier.size(72.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.onPrimary
                ) {
                    IconButton(
                        onClick = { playerController.togglePlayPause() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                IconButton(onClick = { playerController.next() }) {
                    Icon(Icons.Filled.SkipNext, null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(36.dp))
                }
                IconButton(onClick = { playerController.cycleRepeatMode() }) {
                    Icon(
                        when (options.repeatMode) {
                            RepeatModeUi.ONE -> Icons.Filled.RepeatOne
                            else -> Icons.Filled.Repeat
                        },
                        null,
                        tint = if (options.repeatMode != RepeatModeUi.OFF)
                            MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "x%.2f".format(state.playbackSpeed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.clickable {
                        val idx = SPEEDS.indexOfFirst {
                            kotlin.math.abs(it - state.playbackSpeed) < 0.01f
                        }
                        val next = SPEEDS[(idx + 1) % SPEEDS.size]
                        playerController.setPlaybackSpeed(next)
                    }
                )

                Text(
                    "${state.currentIndex + 1}/${state.queue.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )

                Text(
                    when (options.abState) {
                        1 -> "A…"
                        2 -> "A-B"
                        else -> "A-B"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (options.abState > 0) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                    modifier = Modifier.clickable {
                        when (options.abState) {
                            0 -> playerController.setABPointA()
                            1 -> playerController.setABPointB()
                            else -> playerController.clearAB()
                        }
                    }
                )
            }
        }
    }

    if (showSleepSheet) {
        SleepTimerSheet(
            lang = lang,
            onStart = { mode, minutes, wait ->
                when (mode) {
                    0 -> playerController.startSleepTimer(minutes, wait)
                    1 -> playerController.startSleepAtTrackEnd()
                    2 -> playerController.startSleepAtQueueEnd()
                }
                showSleepSheet = false
            },
            onCancelTimer = { playerController.cancelSleepTimer() },
            onDismiss = { showSleepSheet = false }
        )
    }

    if (showPlaylistSheet) {
        ModalBottomSheet(onDismissRequest = { showPlaylistSheet = false }) {
            Column(Modifier.padding(bottom = 32.dp)) {
                Text(
                    Strings.selectPlaylist(lang),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
                if (playlists.isEmpty()) {
                    Text(Strings.noPlaylists(lang),
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn {
                        items(playlists) { playlist ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        scope.launch {
                                            playlistsRepository.addToPlaylist(playlist.id, track)
                                        }
                                        showPlaylistSheet = false
                                    }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.QueueMusic, null,
                                    tint = MaterialTheme.colorScheme.primary)
                                Text(playlist.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(start = 16.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showBookmarksSheet) {
        ModalBottomSheet(onDismissRequest = { showBookmarksSheet = false }) {
            Column(Modifier.padding(bottom = 32.dp)) {
                Text(
                    Strings.bookmarks(lang),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )

                TextButton(
                    onClick = {
                        scope.launch {
                            bookmarksRepository.add(track, state.positionMs)
                        }
                        showBookmarksSheet = false
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                ) {
                    Text("+ ${Strings.bookmarks(lang)}: ${formatDuration(state.positionMs)}")
                }

                if (bookmarks.isEmpty()) {
                    Text(Strings.noResults(lang),
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn {
                        items(bookmarks) { bm ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        playerController.playTracks(listOf(bm.track), 0)
                                        playerController.seekTo(bm.positionMs)
                                        showBookmarksSheet = false
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(bm.track.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(formatDuration(bm.positionMs),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = {
                                    scope.launch { bookmarksRepository.remove(bm.id) }
                                }) {
                                    Icon(Icons.Filled.TimerOff, null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text(Strings.info(lang)) },
            text = {
                Column {
                    Text(track.title, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("${Strings.artists(lang)}: ${track.artist}",
                        style = MaterialTheme.typography.bodyMedium)
                    Text("${Strings.albums(lang)}: ${track.album}",
                        style = MaterialTheme.typography.bodyMedium)
                    Text("${Strings.folders(lang)}: ${track.folderPath ?: "-"}",
                        style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(formatDuration(state.durationMs),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) { Text("OK") }
            }
        )
    }
}

@Composable
private fun WaveformSeekBarWithGestures(
    progress: Float,
    waveform: FloatArray,
    seed: Long,
    playedColor: Color,
    unplayedColor: Color,
    currentPositionMs: Long,
    durationMs: Long,
    onSeek: (Float) -> Unit
) {
    val view = LocalView.current
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }
    var lastSeekTime by remember { mutableLongStateOf(0L) }

    val effectiveProgress = if (isDragging) dragProgress else progress
    val displayPositionMs = if (isDragging) {
        (dragProgress * durationMs).toLong()
    } else {
        currentPositionMs
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .pointerInput(durationMs) {
                    forEachGesture {
                        awaitPointerEventScope {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()

                            isDragging = true
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)

                            var current = (down.position.x / size.width).coerceIn(0f, 1f)
                            dragProgress = current
                            onSeek(current)
                            lastSeekTime = System.currentTimeMillis()

                            var pressed = true
                            while (pressed) {
                                val event = awaitPointerEvent()
                                pressed = event.changes.any { it.pressed }
                                if (pressed) {
                                    val change = event.changes.first()
                                    change.consume()
                                    current = (change.position.x / size.width).coerceIn(0f, 1f)
                                    dragProgress = current

                                    val now = System.currentTimeMillis()
                                    if (now - lastSeekTime > 50L) {
                                        onSeek(current)
                                        lastSeekTime = now
                                    }
                                }
                            }

                            isDragging = false
                            onSeek(dragProgress)
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawWaveform(
                    waveform = waveform,
                    seed = seed,
                    progress = effectiveProgress,
                    playedColor = playedColor,
                    unplayedColor = unplayedColor,
                    barWidth = size.width,
                    barHeight = size.height
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatDuration(displayPositionMs.coerceAtLeast(0L)),
                style = MaterialTheme.typography.labelMedium,
                color = if (isDragging) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatDuration(durationMs.coerceAtLeast(0L)),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun DrawScope.drawWaveform(
    waveform: FloatArray,
    seed: Long,
    progress: Float,
    playedColor: Color,
    unplayedColor: Color,
    barWidth: Float,
    barHeight: Float
) {
    if (waveform.isEmpty()) {
        val random = java.util.Random(seed)
        val binCount = 120
        val binWidth = barWidth / binCount
        val gap = binWidth * 0.2f
        val actualBinWidth = binWidth - gap

        for (i in 0 until binCount) {
            val amplitude = random.nextFloat() * 0.8f + 0.2f
            val h = amplitude * barHeight
            val x = i * binWidth + gap / 2
            val y = (barHeight - h) / 2
            val color = if (i.toFloat() / binCount < progress) playedColor else unplayedColor
            drawRect(color, Offset(x, y), androidx.compose.ui.geometry.Size(actualBinWidth, h))
        }
    } else {
        val binCount = waveform.size
        val binWidth = barWidth / binCount
        val gap = binWidth * 0.2f
        val actualBinWidth = binWidth - gap

        for (i in 0 until binCount) {
            val amplitude = waveform[i]
            val h = amplitude * barHeight
            val x = i * binWidth + gap / 2
            val y = (barHeight - h) / 2
            val color = if (i.toFloat() / binCount < progress) playedColor else unplayedColor
            drawRect(color, Offset(x, y), androidx.compose.ui.geometry.Size(actualBinWidth, h))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SleepTimerSheet(
    lang: AppLanguage,
    onStart: (mode: Int, minutes: Int, wait: Boolean) -> Unit,
    onCancelTimer: () -> Unit,
    onDismiss: () -> Unit
) {
    var mode by remember { mutableIntStateOf(0) }
    var hours by remember { mutableIntStateOf(0) }
    var minutes by remember { mutableIntStateOf(30) }
    var seconds by remember { mutableIntStateOf(0) }
    var wait by remember { mutableStateOf(true) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                Strings.sleepStopTitle(lang),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TimeStepper(hours, 23) { hours = it }
                Text(":", style = MaterialTheme.typography.titleLarge)
                TimeStepper(minutes, 59) { minutes = it }
                Text(":", style = MaterialTheme.typography.titleLarge)
                TimeStepper(seconds, 59) { seconds = it }
            }

            Spacer(Modifier.height(8.dp))

            RadioRow(Strings.sleepAfterTime(lang), mode == 0) { mode = 0 }
            RadioRow(Strings.sleepTrackEnd(lang), mode == 1) { mode = 1 }
            RadioRow(Strings.sleepQueueEnd(lang), mode == 2) { mode = 2 }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    Strings.sleepWait(lang),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Switch(checked = wait, onCheckedChange = { wait = it })
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { onCancelTimer(); onDismiss() }) {
                    Text(Strings.cancel(lang))
                }
                TextButton(
                    onClick = {
                        val total = hours * 60 + minutes + if (seconds > 0) 1 else 0
                        onStart(mode, total.coerceAtLeast(1), wait)
                    }
                ) {
                    Text(Strings.start(lang))
                }
            }
        }
    }
}

@Composable
private fun RadioRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f))
        RadioButton(selected = selected, onClick = onClick)
    }
}

@Composable
private fun TimeStepper(value: Int, max: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onChange((value - 1).coerceIn(0, max)) }) {
            Text("−", style = MaterialTheme.typography.titleMedium)
        }
        Text(
            value.toString().padStart(2, '0'),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.width(48.dp),
            textAlign = TextAlign.Center
        )
        IconButton(onClick = { onChange((value + 1).coerceIn(0, max)) }) {
            Text("+", style = MaterialTheme.typography.titleMedium)
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}