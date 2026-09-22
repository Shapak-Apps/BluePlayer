package com.blueplayer.app.ui.drawer

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.blueplayer.app.ui.navigation.Destinations
import com.blueplayer.core.domain.locale.AppLanguage
import com.blueplayer.core.domain.locale.Strings
import com.blueplayer.core.domain.model.Playlist

@Composable
fun AppDrawerContent(
    currentRoute: String?,
    lang: AppLanguage,
    favoritesCount: Int,
    playlists: List<Playlist>,
    onNavigate: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onAboutClick: () -> Unit,
    onChangelogClick: () -> Unit,
    onExitClick: () -> Unit,
    onCreatePlaylistClick: () -> Unit,
    onDeletePlaylist: (Playlist) -> Unit
) {
    var isMyMusicExpanded by remember { mutableStateOf(false) }
    val isSubRouteActive = currentRoute in listOf(
        Destinations.ARTISTS,
        Destinations.ALBUMS,
        Destinations.LIBRARY,
        Destinations.GENRES,
        Destinations.FOLDERS
    )

    LaunchedEffect(isSubRouteActive) {
        if (isSubRouteActive) {
            isMyMusicExpanded = true
        }
    }

    Column(
        Modifier
            .width(300.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .animateContentSize(
                    animationSpec = tween(
                        durationMillis = 250,
                        easing = LinearOutSlowInEasing
                    )
                )
        ) {
            Spacer(Modifier.height(12.dp))

            DrawerRow(
                icon = Icons.Filled.Home,
                label = Strings.home(lang),
                selected = currentRoute == Destinations.HOME,
                onClick = { onNavigate(Destinations.HOME) }
            )

            MyMusicExpandableRow(
                label = Strings.myMusic(lang),
                expanded = isMyMusicExpanded,
                selected = isSubRouteActive,
                onToggle = { isMyMusicExpanded = !isMyMusicExpanded }
            )

            if (isMyMusicExpanded) {
                Column(
                    modifier = Modifier.animateContentSize(
                        animationSpec = tween(
                            durationMillis = 250,
                            easing = LinearOutSlowInEasing
                        )
                    )
                ) {
                    SubRow(
                        icon = Icons.Filled.Person,
                        label = Strings.artists(lang),
                        selected = currentRoute == Destinations.ARTISTS,
                        onClick = { onNavigate(Destinations.ARTISTS) }
                    )
                    SubRow(
                        icon = Icons.Filled.Album,
                        label = Strings.albums(lang),
                        selected = currentRoute == Destinations.ALBUMS,
                        onClick = { onNavigate(Destinations.ALBUMS) }
                    )
                    SubRow(
                        icon = Icons.Filled.MusicNote,
                        label = Strings.tracks(lang),
                        selected = currentRoute == Destinations.LIBRARY,
                        onClick = { onNavigate(Destinations.LIBRARY) }
                    )
                    SubRow(
                        icon = Icons.Filled.Audiotrack,
                        label = Strings.genres(lang),
                        selected = currentRoute == Destinations.GENRES,
                        onClick = { onNavigate(Destinations.GENRES) }
                    )
                    SubRow(
                        icon = Icons.Filled.Folder,
                        label = Strings.folders(lang),
                        selected = currentRoute == Destinations.FOLDERS,
                        onClick = { onNavigate(Destinations.FOLDERS) }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            DrawerRow(
                icon = Icons.Filled.Tune,
                label = Strings.soundEffects(lang),
                selected = currentRoute == Destinations.EQUALIZER,
                onClick = { onNavigate(Destinations.EQUALIZER) }
            )

            DrawerRow(
                icon = Icons.Filled.FavoriteBorder,
                label = Strings.favorites(lang),
                badge = if (favoritesCount > 0) "$favoritesCount" else null,
                selected = currentRoute == Destinations.FAVORITES,
                onClick = { onNavigate(Destinations.FAVORITES) }
            )

            DrawerRow(
                icon = Icons.Filled.QueueMusic,
                label = Strings.queue(lang),
                selected = currentRoute == Destinations.QUEUE,
                onClick = { onNavigate(Destinations.QUEUE) }
            )

            Spacer(Modifier.height(16.dp))

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${Strings.playlists(lang)}:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onCreatePlaylistClick, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Add, null, modifier = Modifier.size(20.dp))
                }
            }

            playlists.forEach { playlist ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate(Destinations.playlistDetail(playlist.id)) }
                        .height(48.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.width(4.dp).fillMaxHeight())
                    Spacer(Modifier.width(12.dp))
                    Icon(
                        Icons.Filled.QueueMusic,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        playlist.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { onDeletePlaylist(playlist) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                }
            }

            Spacer(Modifier.height(16.dp))
        }

        HorizontalDivider()
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onExitClick) {
                Icon(
                    Icons.Filled.Logout,
                    null,
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                Strings.exit(lang),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onChangelogClick) {
                Icon(
                    Icons.Filled.History,
                    null,
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = onSettingsClick) {
                Icon(
                    Icons.Filled.Settings,
                    null,
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = onAboutClick) {
                Icon(
                    Icons.Filled.Info,
                    null,
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun DrawerRow(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    badge: String? = null,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .height(52.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(
                    if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
                )
        )
        Spacer(Modifier.width(12.dp))
        Icon(
            icon,
            null,
            tint = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.width(16.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (badge != null) {
            Box(
                Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    badge,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.width(12.dp))
        } else {
            Spacer(Modifier.width(12.dp))
        }
    }
}

@Composable
private fun MyMusicExpandableRow(
    label: String,
    expanded: Boolean,
    selected: Boolean,
    onToggle: () -> Unit
) {
    val rotationAngle by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(
            durationMillis = 250,
            easing = LinearOutSlowInEasing
        ),
        label = "chevronRotation"
    )

    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .height(52.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(
                    if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
                )
        )
        Spacer(Modifier.width(12.dp))
        Icon(
            Icons.Filled.LibraryMusic,
            null,
            tint = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.width(16.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Icon(
            Icons.Filled.ExpandMore,
            null,
            modifier = Modifier
                .size(24.dp)
                .padding(end = 16.dp)
                .rotate(rotationAngle),
            tint = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SubRow(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .height(44.dp)
            .padding(start = 52.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            null,
            modifier = Modifier.size(20.dp),
            tint = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(12.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface
        )
    }
}