package com.blueplayer.app.di

import android.content.ComponentName
import android.content.Context
import com.blueplayer.app.MusicPlaybackService
import com.blueplayer.core.data.mediastore.MediaStoreAlbumRepository
import com.blueplayer.core.data.mediastore.MediaStoreArtistRepository
import com.blueplayer.core.data.mediastore.MediaStoreFolderRepository
import com.blueplayer.core.data.mediastore.MediaStoreGenreRepository
import com.blueplayer.core.data.mediastore.MediaStoreTrackRepository
import com.blueplayer.core.data.prefs.QueuePersistenceStore
import com.blueplayer.core.data.prefs.SharedPreferencesLanguageRepository
import com.blueplayer.core.data.prefs.SharedPreferencesSettingsRepository
import com.blueplayer.core.data.prefs.SharedPreferencesThemeRepository
import com.blueplayer.core.database.BookmarksRepositoryImpl
import com.blueplayer.core.database.FavoritesRepositoryImpl
import com.blueplayer.core.database.PlaylistsRepositoryImpl
import com.blueplayer.core.domain.locale.LanguageRepository
import com.blueplayer.core.domain.player.PlayerController
import com.blueplayer.core.domain.repository.AlbumRepository
import com.blueplayer.core.domain.repository.ArtistRepository
import com.blueplayer.core.domain.repository.BookmarksRepository
import com.blueplayer.core.domain.repository.FavoritesRepository
import com.blueplayer.core.domain.repository.FolderRepository
import com.blueplayer.core.domain.repository.GenreRepository
import com.blueplayer.core.domain.repository.PlaylistsRepository
import com.blueplayer.core.domain.repository.SettingsRepository
import com.blueplayer.core.domain.repository.ThemeRepository
import com.blueplayer.core.domain.repository.TrackRepository
import com.blueplayer.core.player.AudioSettingsWatcher
import com.blueplayer.core.player.CoverCache
import com.blueplayer.core.player.CrossfadeMonitor
import com.blueplayer.core.player.EqualizerEngine
import com.blueplayer.core.player.Media3PlayerController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AppContainer(appContext: Context) {

    private val containerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val settingsRepository: SettingsRepository =
        SharedPreferencesSettingsRepository(appContext)

    val trackRepository: TrackRepository =
        MediaStoreTrackRepository(appContext, settingsRepository)
    val albumRepository: AlbumRepository = MediaStoreAlbumRepository(appContext)
    val artistRepository: ArtistRepository = MediaStoreArtistRepository(appContext)
    val folderRepository: FolderRepository = MediaStoreFolderRepository(appContext)

    val themeRepository: ThemeRepository = SharedPreferencesThemeRepository(appContext)
    val languageRepository: LanguageRepository =
        SharedPreferencesLanguageRepository(appContext)

    val queueStore: QueuePersistenceStore = QueuePersistenceStore(appContext)

    val favoritesRepository: FavoritesRepository = FavoritesRepositoryImpl(appContext)
    val playlistsRepository: PlaylistsRepository = PlaylistsRepositoryImpl(appContext)

    val playerController: PlayerController = Media3PlayerController(
        context = appContext,
        serviceComponent = ComponentName(appContext, MusicPlaybackService::class.java)
    )
    val genreRepository: GenreRepository = MediaStoreGenreRepository(appContext)
    val equalizerEngine: EqualizerEngine = EqualizerEngine()

    val bookmarksRepository: BookmarksRepository = BookmarksRepositoryImpl(appContext)

    val coverCache: CoverCache = CoverCache()

    init {
        CrossfadeMonitor.start(
            scope = containerScope,
            controller = playerController,
            settings = settingsRepository.settings
        )
        AudioSettingsWatcher(equalizerEngine).start(
            scope = containerScope,
            settings = settingsRepository.settings
        )
    }
}