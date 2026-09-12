package com.blueplayer.app.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.blueplayer.app.ui.navigation.Destinations
import com.blueplayer.core.domain.player.PlayerController
import com.blueplayer.core.domain.player.PlayerState
import com.blueplayer.core.player.CoverCache
import com.blueplayer.ui.components.GlideArtwork

@Composable
fun MiniPlayerBar(
    state: PlayerState,
    playerController: PlayerController,
    coverCache: CoverCache,
    onExpand: () -> Unit,
    onNavigate: (String) -> Unit,
    onPlusClick: () -> Unit
) {
    val track = state.currentTrack
    val cachedCovers by coverCache.covers.collectAsStateWithLifecycle()
    val coverModel: Any? = track?.let { t ->
        t.artworkUri?.takeIf { it.isNotBlank() } ?: cachedCovers[t.id]
    }

    AnimatedVisibility(visible = track != null) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
        ) {
            val progress = if (state.durationMs > 0)
                state.positionMs.toFloat() / state.durationMs.toFloat() else 0f

            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onExpand)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlideArtwork(
                    model = coverModel,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                )

                Text(
                    track?.title.orEmpty(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )

                IconButton(onClick = { playerController.previous() }) {
                    Icon(Icons.Filled.SkipPrevious, null,
                        tint = MaterialTheme.colorScheme.onPrimary)
                }
                IconButton(onClick = { playerController.togglePlayPause() }) {
                    Icon(
                        if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                IconButton(onClick = { playerController.next() }) {
                    Icon(Icons.Filled.SkipNext, null,
                        tint = MaterialTheme.colorScheme.onPrimary)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPlusClick) {
                    Icon(Icons.Filled.Add, null,
                        tint = MaterialTheme.colorScheme.onPrimary)
                }
                IconButton(onClick = { onNavigate(Destinations.PLAYLISTS) }) {
                    Icon(Icons.Filled.QueueMusic, null,
                        tint = MaterialTheme.colorScheme.onPrimary)
                }
                IconButton(onClick = { onNavigate(Destinations.QUEUE) }) {
                    Icon(Icons.Filled.List, null,
                        tint = MaterialTheme.colorScheme.onPrimary)
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { onNavigate(Destinations.SEARCH) }) {
                    Icon(Icons.Filled.Search, null,
                        tint = MaterialTheme.colorScheme.onPrimary)
                }
                IconButton(onClick = onExpand) {
                    Icon(Icons.Filled.MoreVert, null,
                        tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}