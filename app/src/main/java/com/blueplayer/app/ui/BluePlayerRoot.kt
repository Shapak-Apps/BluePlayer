package com.blueplayer.app.ui

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.blueplayer.app.MusicPlaybackService
import com.blueplayer.app.di.AppContainer
import com.blueplayer.app.ui.drawer.AppDrawerContent
import com.blueplayer.app.ui.navigation.BluePlayerNavHost
import com.blueplayer.app.ui.navigation.Destinations
import com.blueplayer.app.ui.permission.PermissionGate
import com.blueplayer.app.ui.player.MiniPlayerBar
import com.blueplayer.core.domain.locale.AppLanguage
import com.blueplayer.core.domain.locale.Strings
import com.blueplayer.core.domain.model.AccentColor
import com.blueplayer.core.domain.model.Playlist
import com.blueplayer.core.domain.model.ThemeMode
import com.blueplayer.ui.theme.BluePlayerTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluePlayerRoot(
    container: AppContainer,
    startSection: String? = null
) {
    val themeMode by container.themeRepository.themeMode.collectAsStateWithLifecycle()
    val lang by container.languageRepository.language.collectAsStateWithLifecycle()
    val appSettings by container.settingsRepository.settings.collectAsStateWithLifecycle()

    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val seedColor = if (appSettings.accentColor == AccentColor.DEFAULT) null
    else Color(appSettings.accentColor.argb)

    val useDynamicColors = appSettings.dynamicColors && seedColor == null

    BluePlayerTheme(
        darkTheme = darkTheme,
        dynamicColor = useDynamicColors,
        seedColor = seedColor
    ) {
        PermissionGate {
            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            val scope = rememberCoroutineScope()
            val uriHandler = LocalUriHandler.current
            val context = LocalContext.current

            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            val playerState by container.playerController.state.collectAsStateWithLifecycle()
            val playbackOptions by container.playerController.options.collectAsStateWithLifecycle()
            val playlists by container.playlistsRepository.playlists.collectAsStateWithLifecycle()
            val favorites by container.favoritesRepository.favorites.collectAsStateWithLifecycle()

            var showCreatePlaylistDialog by remember { mutableStateOf(false) }
            var deleteTarget by remember { mutableStateOf<Playlist?>(null) }
            var showAddToPlaylistSheet by remember { mutableStateOf(false) }
            var homeTab by remember { mutableIntStateOf(2) }
            var queueRestored by remember { mutableStateOf(false) }

            LaunchedEffect(appSettings.queuePersistence) {
                if (!appSettings.queuePersistence || queueRestored) return@LaunchedEffect
                queueRestored = true
                val saved = container.queueStore.load()
                val empty = container.playerController.state.value.queue.isEmpty()
                if (saved != null && saved.tracks.isNotEmpty() && empty) {
                    container.playerController.playTracks(
                        saved.tracks,
                        saved.index.coerceIn(0, saved.tracks.lastIndex)
                    )
                    container.playerController.seekTo(saved.positionMs)
                    if (!saved.wasPlaying) {
                        container.playerController.togglePlayPause()
                    }
                }
            }

            LaunchedEffect(playerState.queue, playerState.currentIndex, appSettings.queuePersistence) {
                if (appSettings.queuePersistence && playerState.queue.isNotEmpty()) {
                    container.queueStore.saveQueue(
                        playerState.queue,
                        playerState.currentIndex,
                        playerState.isPlaying
                    )
                }
            }

            LaunchedEffect(playerState.isPlaying, appSettings.queuePersistence) {
                if (appSettings.queuePersistence && !playerState.isPlaying) {
                    val s = container.playerController.state.value
                    if (s.queue.isNotEmpty()) {
                        container.queueStore.savePosition(s.positionMs, s.currentIndex)
                    }
                }
            }

            LaunchedEffect(appSettings.queuePersistence) {
                while (appSettings.queuePersistence) {
                    delay(3000)
                    val s = container.playerController.state.value
                    if (s.isPlaying && s.queue.isNotEmpty()) {
                        container.queueStore.savePosition(s.positionMs, s.currentIndex)
                    }
                }
            }

            LaunchedEffect(startSection) {
                if (startSection == null) return@LaunchedEffect
                delay(400)
                when (startSection) {
                    "library" -> {
                        homeTab = 2
                        navController.navigate(Destinations.HOME) { launchSingleTop = true }
                    }
                    "playlists" -> {
                        navController.navigate(Destinations.PLAYLISTS) { launchSingleTop = true }
                    }
                    "settings" -> {
                        navController.navigate(Destinations.SETTINGS) { launchSingleTop = true }
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        AppDrawerContent(
                            currentRoute = currentRoute,
                            lang = lang,
                            favoritesCount = favorites.size,
                            playlists = playlists,
                            onNavigate = { route ->
                                scope.launch { drawerState.close() }

                                val tab = when (route) {
                                    Destinations.ARTISTS -> 0
                                    Destinations.ALBUMS -> 1
                                    Destinations.LIBRARY -> 2
                                    Destinations.GENRES -> 3
                                    Destinations.FOLDERS -> 4
                                    else -> null
                                }

                                if (tab != null) {
                                    homeTab = tab
                                    if (currentRoute != Destinations.HOME) {
                                        navController.navigate(Destinations.HOME) {
                                            popUpTo(Destinations.HOME) { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    }
                                } else if (route != currentRoute) {
                                    navController.navigate(route) {
                                        popUpTo(Destinations.HOME) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            onSettingsClick = {
                                scope.launch { drawerState.close() }
                                navController.navigate(Destinations.SETTINGS)
                            },
                            onAboutClick = {
                                scope.launch { drawerState.close() }
                                navController.navigate(Destinations.ABOUT)
                            },
                            onExitClick = {
                                context.stopService(Intent(context, MusicPlaybackService::class.java))
                                (context as? android.app.Activity)?.finishAffinity()
                            },
                            onCreatePlaylistClick = { showCreatePlaylistDialog = true },
                            onDeletePlaylist = { deleteTarget = it }
                        )
                    }
                ) {
                    Scaffold(
                        topBar = {
                            if (currentRoute != Destinations.SETTINGS &&
                                currentRoute != Destinations.NOW_PLAYING &&
                                currentRoute != Destinations.EQUALIZER &&
                                currentRoute != Destinations.ABOUT &&
                                currentRoute != Destinations.ORGANIZATION
                            ) {
                                TopAppBar(
                                    title = { Text(text = titleForRoute(currentRoute, lang)) },
                                    navigationIcon = {
                                        IconButton(
                                            onClick = { scope.launch { drawerState.open() } }
                                        ) {
                                            Icon(Icons.Filled.Menu, contentDescription = null)
                                        }
                                    },
                                    colors = TopAppBarDefaults.topAppBarColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        },
                        bottomBar = {
                            val miniPlayerHidden =
                                currentRoute == Destinations.SETTINGS ||
                                        currentRoute == Destinations.ABOUT ||
                                        currentRoute == Destinations.ORGANIZATION

                            AnimatedVisibility(
                                visible = !miniPlayerHidden,
                                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                            ) {
                                MiniPlayerBar(
                                    state = playerState,
                                    options = playbackOptions,
                                    playerController = container.playerController,
                                    coverCache = container.coverCache,
                                    onExpand = { navController.navigate(Destinations.NOW_PLAYING) },
                                    onNavigate = { route -> navController.navigate(route) },
                                    onPlusClick = { showAddToPlaylistSheet = true }
                                )
                            }
                        }
                    ) { paddingValues ->
                        BluePlayerNavHost(
                            navController = navController,
                            container = container,
                            lang = lang,
                            onGithubClick = {
                                uriHandler.openUri("https://github.com/aynazar-sylyyew-dev/")
                            },
                            onEqualizerClick = { navController.navigate(Destinations.EQUALIZER) },
                            onOpenDrawer = { scope.launch { drawerState.open() } },
                            modifier = Modifier.padding(paddingValues),
                            homeTab = homeTab,
                            onHomeTabSelected = { homeTab = it },
                        )
                    }
                }
            }

            if (showCreatePlaylistDialog) {
                var name by remember { mutableStateOf("") }

                AlertDialog(
                    onDismissRequest = { showCreatePlaylistDialog = false },
                    title = { Text(Strings.newPlaylist(lang)) },
                    text = {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text(Strings.playlistName(lang)) },
                            singleLine = true
                        )
                    },
                    confirmButton = {
                        TextButton(
                            enabled = name.isNotBlank(),
                            onClick = {
                                scope.launch {
                                    container.playlistsRepository.createPlaylist(name.trim())
                                }
                                showCreatePlaylistDialog = false
                            }
                        ) { Text(Strings.save(lang)) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCreatePlaylistDialog = false }) {
                            Text(Strings.cancel(lang))
                        }
                    }
                )
            }

            deleteTarget?.let { playlist ->
                AlertDialog(
                    onDismissRequest = { deleteTarget = null },
                    title = { Text(Strings.deletePlaylistTitle(lang)) },
                    text = { Text(Strings.deletePlaylistText(lang, playlist.name)) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    container.playlistsRepository.deletePlaylist(playlist.id)
                                }
                                deleteTarget = null
                            }
                        ) { Text(Strings.delete(lang)) }
                    },
                    dismissButton = {
                        TextButton(onClick = { deleteTarget = null }) {
                            Text(Strings.cancel(lang))
                        }
                    }
                )
            }

            if (showAddToPlaylistSheet) {
                val currentTrack = playerState.currentTrack

                if (currentTrack != null) {
                    ModalBottomSheet(
                        onDismissRequest = { showAddToPlaylistSheet = false }
                    ) {
                        Column(Modifier.padding(bottom = 32.dp)) {
                            Text(
                                Strings.selectPlaylist(lang),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                            )

                            if (playlists.isEmpty()) {
                                Text(
                                    Strings.noPlaylists(lang),
                                    modifier = Modifier.padding(16.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                playlists.forEach { playlist ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                scope.launch {
                                                    container.playlistsRepository.addToPlaylist(
                                                        playlist.id, currentTrack
                                                    )
                                                }
                                                showAddToPlaylistSheet = false
                                            }
                                            .padding(horizontal = 16.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Filled.QueueMusic, null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            playlist.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(start = 16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun titleForRoute(route: String?, lang: AppLanguage): String {
    return when (route) {
        Destinations.HOME -> "Blue Player"
        Destinations.LIBRARY -> Strings.tracks(lang)
        Destinations.ALBUMS -> Strings.albums(lang)
        Destinations.ARTISTS -> Strings.artists(lang)
        Destinations.GENRES -> Strings.genres(lang)
        Destinations.FOLDERS -> Strings.folders(lang)
        Destinations.FAVORITES -> Strings.favorites(lang)
        Destinations.QUEUE -> Strings.queue(lang)
        Destinations.PLAYLISTS -> Strings.playlists(lang)
        Destinations.NOW_PLAYING -> Strings.nowPlaying(lang)
        Destinations.SETTINGS -> Strings.settings(lang)
        Destinations.SEARCH -> Strings.search(lang)
        Destinations.EQUALIZER -> Strings.equalizer(lang)
        Destinations.ABOUT -> Strings.about(lang)
        else -> "Blue Player"
    }
}