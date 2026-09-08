package com.blueplayer.app.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.blueplayer.app.di.AppContainer
import com.blueplayer.core.domain.locale.AppLanguage
import com.blueplayer.core.domain.locale.Strings
import com.blueplayer.core.domain.player.PlayerState
import com.blueplayer.feature.albums.AlbumsScreen
import com.blueplayer.feature.albums.AlbumsViewModelFactory
import com.blueplayer.feature.artists.ArtistsScreen
import com.blueplayer.feature.artists.ArtistsViewModelFactory
import com.blueplayer.feature.folders.FoldersScreen
import com.blueplayer.feature.folders.FoldersViewModelFactory
import com.blueplayer.feature.genres.GenresScreen
import com.blueplayer.feature.genres.GenresViewModelFactory
import com.blueplayer.feature.library.LibraryScreen
import com.blueplayer.feature.library.LibraryViewModelFactory

@Composable
fun HomeScreen(
    container: AppContainer,
    lang: AppLanguage,
    playerState: PlayerState,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String) -> Unit
) {
    val tabs = listOf(
        Strings.artists(lang),
        Strings.albums(lang),
        Strings.tracks(lang),
        Strings.genres(lang),
        Strings.folders(lang)
    )

    val libraryVmFactory = remember(container) {
        LibraryViewModelFactory(container.trackRepository, container.playerController)
    }
    val albumsVmFactory = remember(container) {
        AlbumsViewModelFactory(container.albumRepository, container.playerController)
    }
    val artistsVmFactory = remember(container) {
        ArtistsViewModelFactory(container.artistRepository, container.playerController)
    }
    val genresVmFactory = remember(container) {
        GenresViewModelFactory(container.genreRepository, container.playerController)
    }
    val foldersVmFactory = remember(container) {
        FoldersViewModelFactory(container.folderRepository, container.playerController)
    }

    Column(Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { onTabSelected(index) },
                    text = {
                        Text(
                            title,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (selectedTab == index) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                )
            }
        }

        Box(Modifier.fillMaxSize()) {
            when (selectedTab) {
                0 -> ArtistsScreen(artistsVmFactory, lang, onArtistClick = onArtistClick)
                1 -> AlbumsScreen(albumsVmFactory, lang, onAlbumClick = onAlbumClick)
                2 -> LibraryScreen(
                    libraryVmFactory,
                    favoritesRepository = container.favoritesRepository,
                    lang = lang
                )
                3 -> GenresScreen(genresVmFactory, lang)
                4 -> FoldersScreen(foldersVmFactory, playerState, lang)
            }
        }
    }
}