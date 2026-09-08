package com.blueplayer.feature.playlists

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.blueplayer.core.domain.locale.AppLanguage
import com.blueplayer.core.domain.locale.Strings
import com.blueplayer.core.domain.model.Playlist
import com.blueplayer.core.domain.player.PlayerState
import com.blueplayer.ui.components.TrackListItem

@Composable
fun PlaylistsScreen(
    viewModelFactory: ViewModelProvider.Factory,
    playerState: PlayerState,
    lang: AppLanguage,
    modifier: Modifier = Modifier
) {
    val viewModel: PlaylistsViewModel = viewModel(factory = viewModelFactory)
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val detail by viewModel.detail.collectAsStateWithLifecycle()

    var showCreateDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<Playlist?>(null) }
    var deleteTarget by remember { mutableStateOf<Playlist?>(null) }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            if (detail.playlist == null) {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Filled.Add, contentDescription = Strings.create(lang))
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (detail.playlist == null) {
                if (playlists.isEmpty()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.QueueMusic, contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(Strings.noPlaylists(lang),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(Strings.noPlaylistsHint(lang),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(playlists) { playlist ->
                            PlaylistRow(playlist, lang,
                                onClick = { viewModel.openPlaylist(playlist) },
                                onRename = { renameTarget = playlist },
                                onDelete = { deleteTarget = playlist })
                        }
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = viewModel::closePlaylist) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = Strings.back(lang))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(detail.playlist?.name.orEmpty(),
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(Strings.tracksCount(lang, detail.tracks.size),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(onClick = viewModel::playAll) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Text(Strings.play(lang))
                        }
                    }

                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        itemsIndexed(detail.tracks) { index, track ->
                            TrackListItem(
                                track = track,
                                isActive = playerState.currentTrack?.id == track.id,
                                isPlaying = playerState.isPlaying,
                                onClick = { viewModel.playTrack(index) },
                                trailingContent = {
                                    IconButton(onClick = {
                                        detail.playlist?.let {
                                            viewModel.removeTrack(it.id, track.id)
                                        }
                                    }) {
                                        Icon(Icons.Filled.Delete, contentDescription = Strings.delete(lang),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        NameDialog(
            title = Strings.newPlaylist(lang),
            lang = lang,
            onConfirm = { viewModel.create(it) },
            onDismiss = { showCreateDialog = false }
        )
    }

    renameTarget?.let { playlist ->
        NameDialog(
            title = Strings.rename(lang),
            initial = playlist.name,
            lang = lang,
            onConfirm = { viewModel.rename(playlist.id, it) },
            onDismiss = { renameTarget = null }
        )
    }

    deleteTarget?.let { playlist ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(Strings.deletePlaylistTitle(lang)) },
            text = { Text(Strings.deletePlaylistText(lang, playlist.name)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(playlist.id)
                    deleteTarget = null
                }) { Text(Strings.delete(lang)) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text(Strings.cancel(lang)) }
            }
        )
    }
}

@Composable
private fun PlaylistRow(
    playlist: Playlist,
    lang: AppLanguage,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.QueueMusic, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(playlist.name, style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(Strings.tracksCount(lang, playlist.trackCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onRename) {
                Icon(Icons.Filled.Edit, contentDescription = Strings.rename(lang),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = Strings.delete(lang),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun NameDialog(
    title: String,
    initial: String = "",
    lang: AppLanguage,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(Strings.playlistName(lang)) },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(),
                onClick = { onConfirm(name.trim()); onDismiss() }) {
                Text(Strings.save(lang))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(Strings.cancel(lang)) }
        }
    )
}