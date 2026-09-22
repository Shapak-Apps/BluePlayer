package com.blueplayer.feature.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.blueplayer.core.domain.locale.AppLanguage
import com.blueplayer.core.domain.locale.Strings
import com.blueplayer.core.domain.model.Track
import com.blueplayer.core.domain.repository.FavoritesRepository
import com.blueplayer.ui.components.DeleteTrackConfirmDialog
import com.blueplayer.ui.components.ListPreloader
import com.blueplayer.ui.components.TrackListItem
import com.blueplayer.ui.components.rememberTrackDeleterLaunchers
import com.blueplayer.ui.components.storage.TrackDeleter
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@Composable
fun LibraryScreen(
    viewModelFactory: ViewModelProvider.Factory,
    favoritesRepository: FavoritesRepository,
    trackDeleter: TrackDeleter,
    lang: AppLanguage,
    modifier: Modifier = Modifier
) {
    val viewModel: LibraryViewModel = viewModel(factory = viewModelFactory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val favorites by favoritesRepository.favorites.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val listState = rememberLazyListState()
    val context = LocalContext.current
    val density = LocalDensity.current
    val itemSizePx = with(density) { 48.dp.roundToPx() }

    // Shared deletion engine for this screen
    rememberTrackDeleterLaunchers(trackDeleter)
    var pendingDeleteTrack by remember { mutableStateOf<Track?>(null) }

    LaunchedEffect(trackDeleter) {
        trackDeleter.bind(object : TrackDeleter.Callbacks {
            override fun onDeleted(track: Track) {
                viewModel.refresh()
            }

            override fun onFailed(track: Track) { }

            override fun onDenied(track: Track) { }
        })
    }

    // Prefetch artwork for upcoming tracks as the user scrolls.
    LaunchedEffect(listState, uiState) {
        val tracks = (uiState as? LibraryUiState.Success)?.tracks ?: return@LaunchedEffect
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { firstVisible ->
                val prefetchWindow = 12
                val endExclusive = (firstVisible + prefetchWindow + 6)
                    .coerceAtMost(tracks.size)
                for (i in firstVisible until endExclusive) {
                    ListPreloader.preload(context, tracks[i].artworkUri, itemSizePx)
                }
            }
    }

    // Initial prefetch of the first screenful
    LaunchedEffect(uiState) {
        val tracks = (uiState as? LibraryUiState.Success)?.tracks ?: return@LaunchedEffect
        val initial = tracks.take(20)
        initial.forEach { ListPreloader.preload(context, it.artworkUri, itemSizePx) }
    }

    Column(modifier = modifier.fillMaxSize()) {
        LibraryHeader(
            tracks = (uiState as? LibraryUiState.Success)?.tracks.orEmpty()
        )

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            when (val state = uiState) {
                LibraryUiState.Loading -> CircularProgressIndicator()

                is LibraryUiState.Success -> {
                    if (state.tracks.isEmpty()) {
                        Text(Strings.noTracks(lang),
                            style = MaterialTheme.typography.titleMedium)
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            state = listState
                        ) {
                            itemsIndexed(state.tracks) { index, track ->
                                TrackListItem(
                                    track = track,
                                    isActive = playerState.currentTrack?.id == track.id,
                                    isPlaying = playerState.isPlaying,
                                    onClick = { viewModel.play(index) },
                                    isFavorite = favorites.any { it.id == track.id },
                                    onFavoriteClick = {
                                        scope.launch { favoritesRepository.toggleFavorite(track) }
                                    },
                                    onMoreClick = { pendingDeleteTrack = track }
                                )
                            }
                        }
                    }
                }

                is LibraryUiState.Error -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.message, color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp))
                        Button(onClick = viewModel::refresh) { Text(Strings.retry(lang)) }
                    }
                }
            }
        }
    }

    DeleteTrackConfirmDialog(
        track = pendingDeleteTrack,
        lang = lang,
        onConfirm = { track ->
            pendingDeleteTrack = null
            trackDeleter.requestDelete(track)
        },
        onDismiss = { pendingDeleteTrack = null }
    )
}

@Composable
private fun LibraryHeader(tracks: List<Track>) {
    val count = tracks.size
    val totalMs = remember(tracks) { tracks.sumOf { it.durationMs } }
    val totalBytes = remember(tracks) {
        tracks.sumOf { it.sizeBytes ?: 0L }
    }

    val totalSeconds = totalMs / 1000
    val hh = totalSeconds / 3600
    val mm = (totalSeconds % 3600) / 60
    val ss = totalSeconds % 60
    val time = "%02d:%02d:%02d".format(hh, mm, ss)

    val sizeGb = totalBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
    val size = "%.2f GB".format(sizeGb)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = {}) {
            Icon(Icons.Filled.ArrowUpward, null)
        }
        Text(
            "$count / $time / $size",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = { /* к началу */ }) {
            Icon(Icons.Filled.Home, null)
        }
    }
}