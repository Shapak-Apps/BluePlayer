package com.blueplayer.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.blueplayer.app.di.AppContainer
import com.blueplayer.app.ui.home.HomeScreen
import com.blueplayer.core.domain.locale.AppLanguage
import com.blueplayer.feature.albums.AlbumDetailScreen
import com.blueplayer.feature.albums.AlbumDetailViewModelFactory
import com.blueplayer.feature.albums.AlbumsScreen
import com.blueplayer.feature.albums.AlbumsViewModelFactory
import com.blueplayer.feature.artists.ArtistDetailScreen
import com.blueplayer.feature.artists.ArtistDetailViewModelFactory
import com.blueplayer.feature.artists.ArtistsScreen
import com.blueplayer.feature.artists.ArtistsViewModelFactory
import com.blueplayer.feature.favorites.FavoritesScreen
import com.blueplayer.feature.favorites.FavoritesViewModelFactory
import com.blueplayer.feature.folders.FoldersScreen
import com.blueplayer.feature.folders.FoldersViewModelFactory
import com.blueplayer.feature.genres.GenresScreen
import com.blueplayer.feature.genres.GenresViewModelFactory
import com.blueplayer.feature.library.LibraryScreen
import com.blueplayer.feature.library.LibraryViewModelFactory
import com.blueplayer.feature.nowplaying.NowPlayingScreen
import com.blueplayer.feature.playlists.PlaylistsScreen
import com.blueplayer.feature.playlists.PlaylistsViewModelFactory
import com.blueplayer.feature.queue.QueueScreen
import com.blueplayer.feature.search.SearchScreen
import com.blueplayer.feature.search.SearchViewModelFactory
import com.blueplayer.feature.settings.SettingsScreen
import com.blueplayer.feature.settings.SettingsViewModelFactory
import com.blueplayer.feature.settings.equalizer.EqualizerScreen

@Composable
fun BluePlayerNavHost(
    navController: NavHostController,
    container: AppContainer,
    lang: AppLanguage,
    onGithubClick: () -> Unit,
    onEqualizerClick: () -> Unit,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier,
    homeTab: Int,
    onHomeTabSelected: (Int) -> Unit,
) {
    val libraryVmFactory = remember(container) {
        LibraryViewModelFactory(container.trackRepository, container.playerController)
    }
    val albumsVmFactory = remember(container) {
        AlbumsViewModelFactory(container.albumRepository, container.playerController)
    }
    val artistsVmFactory = remember(container) {
        ArtistsViewModelFactory(container.artistRepository, container.playerController)
    }
    val foldersVmFactory = remember(container) {
        FoldersViewModelFactory(container.folderRepository, container.playerController)
    }
    val favoritesVmFactory = remember(container) {
        FavoritesViewModelFactory(container.favoritesRepository, container.playerController)
    }
    val playlistsVmFactory = remember(container) {
        PlaylistsViewModelFactory(container.playlistsRepository, container.playerController)
    }
    val settingsVmFactory = remember(container) {
        SettingsViewModelFactory(container.themeRepository, container.languageRepository)
    }
    val searchVmFactory = remember(container) {
        SearchViewModelFactory(
            container.trackRepository,
            container.albumRepository,
            container.artistRepository,
            container.playerController
        )
    }

    val playerState by container.playerController.state.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Destinations.HOME,
        modifier = modifier
    ) {
        composable(Destinations.HOME) {
            HomeScreen(
                container = container,
                lang = lang,
                playerState = playerState,
                selectedTab = homeTab,
                onTabSelected = onHomeTabSelected,
                onAlbumClick = { navController.navigate(Destinations.albumDetail(it)) },
                onArtistClick = { navController.navigate(Destinations.artistDetail(it)) }
            )
        }
        composable(Destinations.LIBRARY) {
            LibraryScreen(
                viewModelFactory = libraryVmFactory,
                favoritesRepository = container.favoritesRepository,
                lang = lang
            )
        }
        composable(Destinations.ALBUMS) {
            AlbumsScreen(
                viewModelFactory = albumsVmFactory,
                lang = lang,
                onAlbumClick = { navController.navigate(Destinations.albumDetail(it)) }
            )
        }
        composable(Destinations.ARTISTS) {
            ArtistsScreen(
                viewModelFactory = artistsVmFactory,
                lang = lang,
                onArtistClick = { navController.navigate(Destinations.artistDetail(it)) }
            )
        }
        composable(Destinations.GENRES) {
            GenresScreen(
                viewModelFactory = GenresViewModelFactory(
                    container.genreRepository,
                    container.playerController
                ),
                lang = lang
            )
        }
        composable(Destinations.FOLDERS) {
            FoldersScreen(
                viewModelFactory = foldersVmFactory,
                playerState = playerState,
                lang = lang
            )
        }
        composable(Destinations.FAVORITES) {
            FavoritesScreen(
                viewModelFactory = favoritesVmFactory,
                playerState = playerState,
                lang = lang
            )
        }
        composable(Destinations.QUEUE) {
            QueueScreen(
                state = playerState,
                playerController = container.playerController,
                lang = lang
            )
        }
        composable(Destinations.PLAYLISTS) {
            PlaylistsScreen(
                viewModelFactory = playlistsVmFactory,
                playerState = playerState,
                lang = lang
            )
        }
        composable(Destinations.SEARCH) {
            SearchScreen(
                viewModelFactory = searchVmFactory,
                playerState = playerState,
                lang = lang
            )
        }
        composable(Destinations.NOW_PLAYING) {
            val playerState by container.playerController.state.collectAsStateWithLifecycle()
            val playbackOptions by container.playerController.options.collectAsStateWithLifecycle()
            NowPlayingScreen(
                state = playerState,
                options = playbackOptions,
                playerController = container.playerController,
                favoritesRepository = container.favoritesRepository,
                playlistsRepository = container.playlistsRepository,
                bookmarksRepository = container.bookmarksRepository,
                lang = lang,
                onOpenDrawer = onOpenDrawer,
                onEqualizerClick = onEqualizerClick,
                onAlbumClick = { navController.navigate(route = Destinations.albumDetail(albumId = it)) },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Destinations.SETTINGS) {
            SettingsScreen(
                viewModelFactory = settingsVmFactory,
                onBack = { navController.popBackStack() },
                onGithubClick = onGithubClick,
                onEqualizerClick = onEqualizerClick
            )
        }
        composable(Destinations.EQUALIZER) {
            val sessionId by container.playerController.audioSessionId.collectAsState()

            EqualizerScreen(
                engine = container.equalizerEngine,
                sessionId = sessionId,
                playerController = container.playerController,
                lang = lang,
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Destinations.ALBUM_DETAIL,
            arguments = listOf(navArgument("albumId") { type = NavType.StringType })
        ) { backStackEntry ->
            val albumId = backStackEntry.arguments?.getString("albumId") ?: return@composable
            val factory = remember(albumId) {
                AlbumDetailViewModelFactory(albumId, container.albumRepository, container.playerController)
            }
            AlbumDetailScreen(
                viewModelFactory = factory,
                playerState = playerState,
                lang = lang
            )
        }
        composable(
            route = Destinations.ARTIST_DETAIL,
            arguments = listOf(navArgument("artistId") { type = NavType.StringType })
        ) { backStackEntry ->
            val artistId = backStackEntry.arguments?.getString("artistId") ?: return@composable
            val factory = remember(artistId) {
                ArtistDetailViewModelFactory(artistId, container.artistRepository, container.playerController)
            }
            ArtistDetailScreen(
                viewModelFactory = factory,
                playerState = playerState,
                lang = lang
            )
        }
        composable(
            route = Destinations.PLAYLIST_DETAIL,
            arguments = listOf(navArgument("playlistId") { type = NavType.LongType })
        ) { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getLong("playlistId") ?: return@composable
            PlaylistsScreen(
                viewModelFactory = playlistsVmFactory,
                playerState = playerState,
                lang = lang
            )
        }
    }
}