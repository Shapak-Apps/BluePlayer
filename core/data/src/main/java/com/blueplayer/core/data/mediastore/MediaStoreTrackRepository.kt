package com.blueplayer.core.data.mediastore

import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.blueplayer.core.domain.model.AppSettings
import com.blueplayer.core.domain.model.SortOrder
import com.blueplayer.core.domain.model.Track
import com.blueplayer.core.domain.repository.SettingsRepository
import com.blueplayer.core.domain.repository.TrackRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MediaStoreTrackRepository(
    private val context: Context,
    private val settingsRepository: SettingsRepository
) : TrackRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val allTracks = MutableStateFlow<List<Track>>(emptyList())

    private val _tracks = MutableStateFlow<List<Track>>(emptyList())
    override val tracks: StateFlow<List<Track>> = _tracks.asStateFlow()

    init {
        scope.launch {
            combine(allTracks, settingsRepository.settings) { list, settings ->
                applyFilters(list, settings)
            }.collect { filtered ->
                _tracks.value = filtered
            }
        }

        scope.launch {
            observeChanges().collect {
                rescan()
            }
        }

        scope.launch {
            rescan()
        }
    }

    override suspend fun rescan() {
        val loaded = loadAllTracks()
        allTracks.value = loaded
    }

    private suspend fun loadAllTracks(): List<Track> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<Track>()

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ARTIST_ID,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_ADDED
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        val albumArtBaseUri = Uri.parse("content://media/external/audio/albumart")

        try {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->

                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val artistIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

                while (cursor.moveToNext()) {
                    val dataPath = cursor.getString(dataColumn)

                    if (dataPath == null || !File(dataPath).exists()) continue

                    val id = cursor.getLong(idColumn)
                    val title = cursor.getString(titleColumn) ?: "Unknown"
                    val artist = cursor.getString(artistColumn) ?: "Unknown"
                    val artistId = cursor.getLong(artistIdColumn).toString()
                    val album = cursor.getString(albumColumn) ?: ""
                    val albumId = cursor.getLong(albumIdColumn).toString()
                    val duration = cursor.getLong(durationColumn)
                    val size = cursor.getLong(sizeColumn)
                    val dateAdded = cursor.getLong(dateAddedColumn)
                    val folderPath = File(dataPath).parent

                    val uri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                    )
                    val artworkUri = ContentUris.withAppendedId(
                        albumArtBaseUri, albumId.toLongOrNull() ?: 0L
                    )

                    tracks.add(
                        Track(
                            id = id.toString(),
                            title = title,
                            artist = artist,
                            artistId = artistId,
                            album = album,
                            albumId = albumId,
                            durationMs = duration,
                            uri = uri.toString(),
                            artworkUri = artworkUri.toString(),
                            folderPath = folderPath,
                            sizeBytes = size,
                            dateAdded = dateAdded
                        )
                    )
                }
            }
        } catch (e: Exception) {
            return@withContext emptyList()
        }

        tracks
    }

    private fun applyFilters(tracks: List<Track>, settings: AppSettings): List<Track> {
        val minDurationMs = settings.minTrackDurationSeconds * 1000L
        val excludedFolders = settings.excludedFolders.map { it.lowercase() }

        val filtered = tracks.filter { track ->
            if (minDurationMs > 0 && track.durationMs < minDurationMs) return@filter false

            val folderName = track.folderPath?.substringAfterLast('/')?.lowercase() ?: ""
            if (folderName in excludedFolders) return@filter false

            true
        }

        return sortTracks(filtered, settings.defaultSortOrder)
    }

    private fun sortTracks(tracks: List<Track>, order: SortOrder): List<Track> {
        return when (order) {
            SortOrder.TITLE_ASC -> tracks.sortedBy { it.title.lowercase() }
            SortOrder.TITLE_DESC -> tracks.sortedByDescending { it.title.lowercase() }
            SortOrder.DATE_ADDED_DESC -> tracks.sortedByDescending { it.dateAdded }
            SortOrder.DATE_ADDED_ASC -> tracks.sortedBy { it.dateAdded }
            SortOrder.DURATION_ASC -> tracks.sortedBy { it.durationMs }
            SortOrder.DURATION_DESC -> tracks.sortedByDescending { it.durationMs }
            SortOrder.ARTIST_ASC -> tracks.sortedBy { it.artist.lowercase() }
            SortOrder.ALBUM_ASC -> tracks.sortedBy { it.album.lowercase() }
        }
    }

    override suspend fun getTracks(): List<Track> = _tracks.value

    override suspend fun searchTracks(query: String): List<Track> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()

        _tracks.value.filter { track ->
            track.title.contains(query, ignoreCase = true) ||
                    track.artist.contains(query, ignoreCase = true)
        }
    }

    override fun observeChanges(): Flow<Long> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(System.currentTimeMillis())
            }
        }

        context.contentResolver.registerContentObserver(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            true,
            observer
        )

        awaitClose {
            context.contentResolver.unregisterContentObserver(observer)
        }
    }
}