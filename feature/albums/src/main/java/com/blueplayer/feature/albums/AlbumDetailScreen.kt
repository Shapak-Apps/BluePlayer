package com.blueplayer.feature.albums

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.blueplayer.core.domain.locale.AppLanguage
import com.blueplayer.core.domain.locale.Strings
import com.blueplayer.core.domain.player.PlayerState
import com.blueplayer.ui.components.ArtworkPlaceholder
import com.blueplayer.ui.components.TrackListItem

@Composable
fun AlbumDetailScreen(
    viewModelFactory: ViewModelProvider.Factory,
    playerState: PlayerState,
    lang: AppLanguage,
    modifier: Modifier = Modifier
) {
    val viewModel: AlbumDetailViewModel = viewModel(factory = viewModelFactory)
    val state by viewModel.state.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (state.isLoading) {
            CircularProgressIndicator()
        } else {
            val album = state.album
            if (album != null) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier.size(200.dp).clip(RoundedCornerShape(16.dp))
                            ) {
                                if (album.artworkUri != null) {
                                    AsyncImage(album.artworkUri, album.title,
                                        Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop)
                                } else {
                                    ArtworkPlaceholder(Modifier.fillMaxSize())
                                }
                            }

                            Spacer(Modifier.height(16.dp))
                            Text(album.title, style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface)
                            Text(album.artist, style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(Strings.tracksCount(lang, album.trackCount),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)

                            Spacer(Modifier.height(16.dp))
                            Button(onClick = viewModel::playAll) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                Spacer(Modifier.size(8.dp))
                                Text(Strings.playAll(lang))
                            }
                        }
                    }

                    itemsIndexed(state.tracks) { index, track ->
                        TrackListItem(
                            track = track,
                            isActive = playerState.currentTrack?.id == track.id,
                            isPlaying = playerState.isPlaying,
                            onClick = { viewModel.playTrack(index) }
                        )
                    }
                }
            }
        }
    }
}