package com.blueplayer.feature.genres

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.blueplayer.core.domain.locale.AppLanguage
import com.blueplayer.core.domain.locale.Strings
import com.blueplayer.ui.components.TrackListItem

@Composable
fun GenresScreen(
    viewModelFactory: ViewModelProvider.Factory,
    lang: AppLanguage,
    modifier: Modifier = Modifier
) {
    val viewModel: GenresViewModel = viewModel(factory = viewModelFactory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val detail by viewModel.detail.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when {
            detail.genre != null -> {
                Column(Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = viewModel::closeGenre) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(detail.genre?.name.orEmpty(),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface)
                            Text(Strings.tracksCount(lang, detail.tracks.size),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(onClick = viewModel::playAll) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Text(Strings.play(lang))
                        }
                    }
                    if (detail.isLoading) {
                        CircularProgressIndicator()
                    } else {
                        GenreTracksPlaceholder(viewModel, detail, lang)
                    }
                }
            }
            else -> {
                when (val state = uiState) {
                    GenresUiState.Loading -> CircularProgressIndicator()
                    is GenresUiState.Success -> {
                        if (state.genres.isEmpty()) {
                            Text(Strings.noResults(lang),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface)
                        } else {
                            LazyColumn(Modifier.fillMaxSize()) {
                                items(state.genres) { genre ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth()
                                            .clickable { viewModel.openGenre(genre) }
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier.size(48.dp)
                                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Filled.LibraryMusic, null,
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                        }
                                        Column(Modifier.padding(start = 16.dp)) {
                                            Text(genre.name,
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurface)
                                            Text(Strings.tracksCount(lang, genre.trackCount),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    is GenresUiState.Error -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(state.message, color = MaterialTheme.colorScheme.error)
                            Button(onClick = viewModel::refresh) { Text(Strings.retry(lang)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GenreTracksPlaceholder(
    viewModel: GenresViewModel,
    detail: GenreDetailState,
    lang: AppLanguage
) {
    LazyColumn(Modifier.fillMaxSize()) {
        itemsIndexed(detail.tracks) { index, track ->
            TrackListItem(
                track = track,
                isActive = false,
                isPlaying = false,
                onClick = { viewModel.playTrack(index) }
            )
        }
    }
}