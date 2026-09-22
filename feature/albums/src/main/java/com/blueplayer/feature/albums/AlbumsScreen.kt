package com.blueplayer.feature.albums

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.blueplayer.core.domain.locale.AppLanguage
import com.blueplayer.core.domain.locale.Strings
import com.blueplayer.core.domain.model.Album
import com.blueplayer.ui.components.ArtworkPlaceholder
import com.blueplayer.ui.components.ListPreloader
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun AlbumsScreen(
    viewModelFactory: ViewModelProvider.Factory,
    lang: AppLanguage,
    onAlbumClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: AlbumsViewModel = viewModel(factory = viewModelFactory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val gridState = rememberLazyGridState()
    val context = LocalContext.current
    val density = LocalDensity.current
    val itemSizePx = with(density) { 130.dp.roundToPx() }

    // Prefetch album covers for cells approaching the viewport.
    // Grid shows ~12 cells per screen on phones, so prefetch 16 ahead
    // to cover fast flings and orientation changes.
    LaunchedEffect(gridState, uiState) {
        val albums = (uiState as? AlbumsUiState.Success)?.albums ?: return@LaunchedEffect
        snapshotFlow { gridState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { firstVisible ->
                val prefetchWindow = 16
                val endExclusive = (firstVisible + prefetchWindow + 8)
                    .coerceAtMost(albums.size)
                for (i in firstVisible until endExclusive) {
                    ListPreloader.preload(context, albums[i].artworkUri, itemSizePx)
                }
            }
    }

    // Initial prefetch of the first visible grid so the opening frame
    // renders decoded bitmaps instead of placeholders.
    LaunchedEffect(uiState) {
        val albums = (uiState as? AlbumsUiState.Success)?.albums ?: return@LaunchedEffect
        albums.take(24).forEach {
            ListPreloader.preload(context, it.artworkUri, itemSizePx)
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (val state = uiState) {
            AlbumsUiState.Loading -> CircularProgressIndicator()
            is AlbumsUiState.Success -> {
                if (state.albums.isEmpty()) {
                    Text(Strings.noAlbums(lang),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface)
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 130.dp),
                        modifier = Modifier.fillMaxSize(),
                        state = gridState,
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.albums) { album ->
                            AlbumCard(
                                album = album,
                                lang = lang,
                                onClick = { onAlbumClick(album.id) },
                                onPlay = { viewModel.playAlbum(album.id) }
                            )
                        }
                    }
                }
            }
            is AlbumsUiState.Error -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                    Button(onClick = viewModel::refresh) { Text(Strings.retry(lang)) }
                }
            }
        }
    }
}

@Composable
private fun AlbumCard(
    album: Album,
    lang: AppLanguage,
    onClick: () -> Unit,
    onPlay: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
                if (album.artworkUri != null) {
                    AsyncImage(
                        model = album.artworkUri,
                        contentDescription = album.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    ArtworkPlaceholder(Modifier.fillMaxSize())
                }
                Box(modifier = Modifier.align(Alignment.TopEnd)) {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface)
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text(Strings.play(lang)) },
                            leadingIcon = { Icon(Icons.Filled.PlayArrow, null) },
                            onClick = { menuExpanded = false; onPlay() }
                        )
                    }
                }
            }
            Column(modifier = Modifier.padding(10.dp)) {
                Text(album.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Text(album.artist,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}