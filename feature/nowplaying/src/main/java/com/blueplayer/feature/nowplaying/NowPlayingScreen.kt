package com.blueplayer.feature.nowplaying

import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.blueplayer.core.domain.locale.AppLanguage
import com.blueplayer.core.domain.locale.Strings
import com.blueplayer.core.domain.model.AppSettings
import com.blueplayer.core.domain.model.CoverShape
import com.blueplayer.core.domain.model.Track
import com.blueplayer.core.domain.player.PlaybackOptions
import com.blueplayer.core.domain.player.PlayerController
import com.blueplayer.core.domain.player.PlayerState
import com.blueplayer.core.domain.player.RepeatModeUi
import com.blueplayer.core.domain.repository.BookmarksRepository
import com.blueplayer.core.domain.repository.FavoritesRepository
import com.blueplayer.core.domain.repository.PlaylistsRepository
import com.blueplayer.core.player.CoverCache
import com.blueplayer.core.player.OnlineCoverFetcher
import com.blueplayer.core.player.WaveformExtractor
import com.blueplayer.ui.components.ArtworkPlaceholder
import com.blueplayer.ui.components.GlideArtwork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private val SPEEDS = listOf(0.75f, 1f, 1.25f, 1.5f, 2f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    state: PlayerState,
    options: PlaybackOptions,
    settings: AppSettings,
    playerController: PlayerController,
    coverCache: CoverCache,
    favoritesRepository: FavoritesRepository,
    playlistsRepository: PlaylistsRepository,
    bookmarksRepository: BookmarksRepository,
    lang: AppLanguage,
    isFavorite: Boolean,
    onOpenDrawer: () -> Unit,
    onEqualizerClick: () -> Unit,
    onAlbumClick: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onPlusClick: () -> Unit,
    onPlaylistsClick: () -> Unit,
    onQueueClick: () -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val track = state.currentTrack
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val contentResolver = context.contentResolver

    LaunchedEffect(settings.keepScreenOn, state.isPlaying) {
        view.keepScreenOn = settings.keepScreenOn && state.isPlaying
    }

    val favorites by favoritesRepository.favorites.collectAsStateWithLifecycle()
    val playlists by playlistsRepository.playlists.collectAsStateWithLifecycle()
    val bookmarks by bookmarksRepository.bookmarks.collectAsStateWithLifecycle()

    val coverFetcher = remember { OnlineCoverFetcher(context, coverCache) }

    val cachedCovers by coverCache.covers.collectAsStateWithLifecycle()

    val coverModel: Any? = track?.let { t ->
        val online = cachedCovers[t.id] as? String
        if (online != null) {
            online
        } else {
            t.artworkUri?.takeIf { it.isNotBlank() }
        }
    }

    LaunchedEffect(track?.id, settings.onlineCoversEnabled) {
        val t = track ?: return@LaunchedEffect
        if (settings.onlineCoversEnabled) {
            coverFetcher.getCoverUrl(t.id, t.title, t.artist)
        }
    }

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

    var showSleepSheet by remember { mutableStateOf(false) }
    var showPlaylistSheet by remember { mutableStateOf(false) }
    var showBookmarksSheet by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    var pendingDeleteUri by remember { mutableStateOf<Uri?>(null) }
    var pendingDeleteTrack by remember { mutableStateOf<Track?>(null) }

    suspend fun performDelete(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val crDeleted = runCatching { contentResolver.delete(uri, null, null) > 0 }
            .getOrDefault(false)
        if (crDeleted) return@withContext true

        val docDeleted = runCatching { DocumentFile.fromSingleUri(context, uri)?.delete() == true }
            .getOrDefault(false)
        if (docDeleted) return@withContext true

        val path = if (uri.scheme == "file") uri.path else getRealPathFromUri(context, uri)
        if (path != null) {
            val file = File(path)
            if (file.exists() && file.delete()) return@withContext true
        }
        false
    }

    fun cleanupAfterDelete(t: Track) {
        scope.launch {
            if (favorites.any { it.id == t.id }) {
                favoritesRepository.toggleFavorite(t)
            }
            bookmarks.filter { it.track.id == t.id }.forEach { bm ->
                bookmarksRepository.remove(bm.id)
            }
            playlists.forEach { p ->
                playlistsRepository.removeFromPlaylist(p.id, t.id)
            }
            playerController.next()
            onNavigateBack()
        }
    }

    fun onDeletedOk(t: Track) {
        Toast.makeText(context, Strings.trackDeleted(lang), Toast.LENGTH_SHORT).show()
        cleanupAfterDelete(t)
    }

    fun onDeletedFail() {
        Toast.makeText(context, Strings.deleteFailed(lang), Toast.LENGTH_SHORT).show()
    }

    fun onDeletedDenied() {
        Toast.makeText(context, Strings.deleteNotAllowed(lang), Toast.LENGTH_LONG).show()
    }

    val intentSenderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val uri = pendingDeleteUri
        val t = pendingDeleteTrack
        pendingDeleteUri = null
        pendingDeleteTrack = null
        if (uri == null || t == null) return@rememberLauncherForActivityResult

        if (result.resultCode == Activity.RESULT_OK) {
            scope.launch {
                val deleted = withContext(Dispatchers.IO) {
                    var stillExists = runCatching {
                        contentResolver.query(
                            uri,
                            arrayOf(MediaStore.MediaColumns._ID),
                            null, null, null
                        )?.use { it.moveToFirst() } ?: false
                    }.getOrDefault(false)

                    if (stillExists) {
                        stillExists = !performDelete(uri)
                    }
                    if (stillExists) {
                        val path = if (uri.scheme == "file") uri.path
                        else getRealPathFromUri(context, uri)
                        stillExists = path != null && File(path).exists()
                    }
                    !stillExists
                }
                if (deleted) onDeletedOk(t) else onDeletedFail()
            }
        } else {
            onDeletedDenied()
        }
    }

    val writePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val uri = pendingDeleteUri
        val t = pendingDeleteTrack
        pendingDeleteUri = null
        pendingDeleteTrack = null
        if (uri == null || t == null) return@rememberLauncherForActivityResult

        if (!granted) {
            onDeletedDenied()
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            val deleted = performDelete(uri)
            if (deleted) onDeletedOk(t) else onDeletedFail()
        }
    }

    val allFilesAccessLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        val uri = pendingDeleteUri
        val t = pendingDeleteTrack
        pendingDeleteUri = null
        pendingDeleteTrack = null
        if (uri == null || t == null) return@rememberLauncherForActivityResult

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
            scope.launch {
                val deleted = performDelete(uri)
                if (deleted) onDeletedOk(t) else onDeletedFail()
            }
        } else {
            onDeletedDenied()
        }
    }

    fun requestAllFilesAccess(uri: Uri, t: Track) {
        pendingDeleteUri = uri
        pendingDeleteTrack = t
        runCatching {
            allFilesAccessLauncher.launch(
                Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
            )
        }.onFailure {
            pendingDeleteUri = null
            pendingDeleteTrack = null
            onDeletedFail()
        }
    }

    fun fallbackDelete(uri: Uri, t: Track) {
        scope.launch {
            val deleted = performDelete(uri)
            if (deleted) {
                onDeletedOk(t)
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                    !Environment.isExternalStorageManager()
                ) {
                    requestAllFilesAccess(uri, t)
                } else {
                    onDeletedFail()
                }
            }
        }
    }

    fun launchMediaStoreDeleteRequest(uri: Uri, t: Track) {
        pendingDeleteUri = uri
        pendingDeleteTrack = t

        val request = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching {
                MediaStore.createDeleteRequest(contentResolver, listOf(uri))
            }.getOrNull()
        } else {
            null
        }

        if (request == null) {
            pendingDeleteUri = null
            pendingDeleteTrack = null
            fallbackDelete(uri, t)
            return
        }

        runCatching {
            intentSenderLauncher.launch(
                IntentSenderRequest.Builder(request.intentSender).build()
            )
        }.onFailure {
            pendingDeleteUri = null
            pendingDeleteTrack = null
            fallbackDelete(uri, t)
        }
    }

    fun requestDelete(t: Track) {
        val originalUri = runCatching { Uri.parse(t.uri) }.getOrNull()
        if (originalUri == null) {
            onDeletedFail()
            return
        }

        if (originalUri.scheme == "file") {
            scope.launch {
                val deleted = performDelete(originalUri)
                if (deleted) {
                    onDeletedOk(t)
                } else {
                    when {
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                            if (Environment.isExternalStorageManager()) onDeletedFail()
                            else requestAllFilesAccess(originalUri, t)
                        }
                        Build.VERSION.SDK_INT > Build.VERSION_CODES.P -> onDeletedDenied()
                        else -> {
                            pendingDeleteUri = originalUri
                            pendingDeleteTrack = t
                            writePermissionLauncher.launch(
                                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                            )
                        }
                    }
                }
            }
            return
        }

        val mediaUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            originalUri.scheme == "content" &&
            originalUri.authority != MediaStore.AUTHORITY
        ) {
            runCatching { MediaStore.getMediaUri(context, originalUri) }.getOrNull()
                ?: originalUri
        } else {
            originalUri
        }

        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                if (Environment.isExternalStorageManager()) {
                    scope.launch {
                        val deleted = performDelete(mediaUri)
                        if (deleted) onDeletedOk(t)
                        else launchMediaStoreDeleteRequest(mediaUri, t)
                    }
                } else {
                    launchMediaStoreDeleteRequest(mediaUri, t)
                }
            }

            Build.VERSION.SDK_INT == Build.VERSION_CODES.Q -> {
                scope.launch {
                    try {
                        val deleted = withContext(Dispatchers.IO) {
                            contentResolver.delete(mediaUri, null, null) > 0
                        }
                        if (deleted) {
                            onDeletedOk(t)
                        } else {
                            val docDeleted = withContext(Dispatchers.IO) {
                                runCatching {
                                    DocumentFile.fromSingleUri(context, mediaUri)?.delete() == true
                                }.getOrDefault(false)
                            }
                            if (docDeleted) onDeletedOk(t) else onDeletedFail()
                        }
                    } catch (e: RecoverableSecurityException) {
                        pendingDeleteUri = mediaUri
                        pendingDeleteTrack = t
                        runCatching {
                            intentSenderLauncher.launch(
                                IntentSenderRequest.Builder(
                                    e.userAction.actionIntent.intentSender
                                ).build()
                            )
                        }.onFailure {
                            pendingDeleteUri = null
                            pendingDeleteTrack = null
                            onDeletedFail()
                        }
                    } catch (e: SecurityException) {
                        val docDeleted = withContext(Dispatchers.IO) {
                            runCatching {
                                DocumentFile.fromSingleUri(context, mediaUri)?.delete() == true
                            }.getOrDefault(false)
                        }
                        if (docDeleted) onDeletedOk(t) else onDeletedDenied()
                    } catch (e: Exception) {
                        onDeletedFail()
                    }
                }
            }

            else -> {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED

                if (granted) {
                    scope.launch {
                        val deleted = performDelete(originalUri)
                        if (deleted) onDeletedOk(t) else onDeletedFail()
                    }
                } else {
                    pendingDeleteUri = originalUri
                    pendingDeleteTrack = t
                    writePermissionLauncher.launch(
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                }
            }
        }
    }

    if (track == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(Strings.nothingPlaying(lang), style = MaterialTheme.typography.titleLarge)
        }
        return
    }

    val seed = track.id.toLongOrNull() ?: track.title.hashCode().toLong()

    val artworkShape = when (settings.coverShape) {
        CoverShape.ROUNDED -> RoundedCornerShape(16.dp)
        CoverShape.CIRCLE -> CircleShape
        CoverShape.SQUARE -> RoundedCornerShape(0.dp)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Top bar: drawer + track name/artist (first line) + actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onOpenDrawer) {
                Icon(Icons.Filled.Menu, null)
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
            ) {
                Text(
                    track.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    track.artist,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onToggleFavorite) {
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

        // Main content: enlarged artwork (MiniPlayerBar is hidden on this screen)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(1f, matchHeightConstraintsFirst = true)
                        .shadow(16.dp, artworkShape),
                    shape = artworkShape,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onLongPress = {
                                        if (settings.hapticFeedback) {
                                            view.performHapticFeedback(
                                                HapticFeedbackConstants.LONG_PRESS
                                            )
                                        }
                                        showDeleteDialog = true
                                    }
                                )
                            }
                    ) {
                        ArtworkPlaceholder(Modifier.fillMaxSize())
                        if (coverModel != null) {
                            GlideArtwork(
                                model = coverModel,
                                modifier = Modifier.fillMaxSize(),
                                onError = { }
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
                                onToggleFavorite()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(Strings.goToAlbum(lang)) },
                            onClick = { menuExpanded = false; onAlbumClick(track.albumId) }
                        )
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(Icons.Filled.Speed, null,
                                    tint = MaterialTheme.colorScheme.primary)
                            },
                            text = { Text("x%.2f".format(state.playbackSpeed)) },
                            onClick = {
                                menuExpanded = false
                                val idx = SPEEDS.indexOfFirst {
                                    kotlin.math.abs(it - state.playbackSpeed) < 0.01f
                                }
                                val next = SPEEDS[(idx + 1) % SPEEDS.size]
                                playerController.setPlaybackSpeed(next)
                            }
                        )
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    if (options.abState > 0) Icons.Filled.RepeatOne
                                    else Icons.Filled.Repeat,
                                    null,
                                    tint = if (options.abState > 0)
                                        MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            text = {
                                Text(
                                    when (options.abState) {
                                        1 -> "A…"
                                        2 -> "A-B"
                                        else -> "A-B"
                                    }
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                when (options.abState) {
                                    0 -> playerController.setABPointA()
                                    1 -> playerController.setABPointB()
                                    else -> playerController.clearAB()
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(Strings.refreshCover(lang)) },
                            onClick = {
                                menuExpanded = false
                                coverFetcher.clearCacheForTrack(track.id)
                                scope.launch {
                                    coverFetcher.getCoverUrl(track.id, track.title, track.artist)
                                    Toast.makeText(context, Strings.coverRefreshed(lang), Toast.LENGTH_SHORT).show()
                                }
                            }
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
                        DropdownMenuItem(
                            text = {
                                Text(
                                    Strings.deleteFromDevice(lang),
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Delete,
                                    null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = { menuExpanded = false; showDeleteDialog = true }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            val seekProgress = if (state.durationMs > 0)
                state.positionMs.toFloat() / state.durationMs.toFloat() else 0f

            if (settings.showWaveform) {
                WaveformSeekBar(
                    progress = seekProgress,
                    waveform = waveform,
                    seed = seed,
                    playedColor = MaterialTheme.colorScheme.primary,
                    unplayedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                    currentPositionMs = state.positionMs,
                    durationMs = state.durationMs,
                    onSeek = { f: Float -> playerController.seekTo((f * state.durationMs).toLong()) },
                    hapticEnabled = settings.hapticFeedback
                )
            } else {
                SimpleSeekBar(
                    progress = seekProgress,
                    currentPositionMs = state.positionMs,
                    durationMs = state.durationMs,
                    onSeek = { f: Float -> playerController.seekTo((f * state.durationMs).toLong()) }
                )
            }

            Spacer(Modifier.height(12.dp))
        }

        // Transport controls (big play row) + secondary action row
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

            Spacer(Modifier.height(4.dp))

            // Secondary row: same actions as the MiniPlayerBar second row
            // (hidden on this screen). Navigation is done by the caller
            // via callbacks, so this module never imports app classes.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { playerController.cycleRepeatMode() }) {
                    Icon(
                        if (options.repeatMode == RepeatModeUi.ONE)
                            Icons.Filled.RepeatOne
                        else
                            Icons.Filled.Repeat,
                        null,
                        tint = if (options.repeatMode == RepeatModeUi.OFF)
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                        else
                            MaterialTheme.colorScheme.onPrimary
                    )
                }
                IconButton(onClick = onPlusClick) {
                    Icon(Icons.Filled.Add, null,
                        tint = MaterialTheme.colorScheme.onPrimary)
                }
                IconButton(onClick = onPlaylistsClick) {
                    Icon(Icons.Filled.QueueMusic, null,
                        tint = MaterialTheme.colorScheme.onPrimary)
                }
                IconButton(onClick = onQueueClick) {
                    Icon(Icons.Filled.List, null,
                        tint = MaterialTheme.colorScheme.onPrimary)
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onSearchClick) {
                    Icon(Icons.Filled.Search, null,
                        tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(Strings.deleteTrackTitle(lang)) },
            text = { Text(Strings.deleteTrackText(lang, track.title)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        requestDelete(track)
                    }
                ) {
                    Text(Strings.delete(lang), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(Strings.cancel(lang))
                }
            }
        )
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

private fun getRealPathFromUri(context: android.content.Context, uri: Uri): String? {
    return try {
        val projection = arrayOf(MediaStore.Audio.Media.DATA)
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            if (cursor.moveToFirst()) cursor.getString(columnIndex) else null
        }
    } catch (e: Exception) {
        null
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}