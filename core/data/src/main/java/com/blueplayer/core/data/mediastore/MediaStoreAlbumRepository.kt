package com.blueplayer.core.data.mediastore

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.blueplayer.core.domain.model.Album
import com.blueplayer.core.domain.model.Track
import com.blueplayer.core.domain.repository.AlbumRepository
import com.blueplayer.core.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File

class MediaStoreAlbumRepository(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val onlineCovers: StateFlow<Map<String, Any>>
) : AlbumRepository {

    private val albumArtBaseUri = Uri.parse("content://media/external/audio/albumart")

    override suspend fun getAlbums(): List<Album> = withContext(Dispatchers.IO) {
        val albums = mutableListOf<Album>()

        val projection = arrayOf(
            MediaStore.Audio.Albums._ID,
            MediaStore.Audio.Albums.ALBUM,
            MediaStore.Audio.Albums.ARTIST,
            MediaStore.Audio.Albums.NUMBER_OF_SONGS
        )

        val sortOrder = "${MediaStore.Audio.Albums.ALBUM} ASC"

        try {
            context.contentResolver.query(
                MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->

                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST)
                val countColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.NUMBER_OF_SONGS)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val title = cursor.getString(albumColumn) ?: "Unknown Album"
                    val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                    val count = cursor.getInt(countColumn)

                    val artworkUri = ContentUris.withAppendedId(albumArtBaseUri, id)

                    albums.add(
                        Album(
                            id = id.toString(),
                            title = title,
                            artist = artist,
                            artworkUri = artworkUri.toString(),
                            trackCount = count
                        )
                    )
                }
            }
        } catch (e: Exception) {
            return@withContext emptyList()
        }

        albums
    }

    override suspend fun searchAlbums(query: String): List<Album> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()

        val albums = mutableListOf<Album>()
        val selection = "${MediaStore.Audio.Albums.ALBUM} LIKE ?"
        val selectionArgs = arrayOf("%$query%")

        try {
            context.contentResolver.query(
                MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                arrayOf(
                    MediaStore.Audio.Albums._ID,
                    MediaStore.Audio.Albums.ALBUM,
                    MediaStore.Audio.Albums.ARTIST,
                    MediaStore.Audio.Albums.NUMBER_OF_SONGS
                ),
                selection,
                selectionArgs,
                "${MediaStore.Audio.Albums.ALBUM} ASC LIMIT 50"
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST)
                val countColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.NUMBER_OF_SONGS)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val title = cursor.getString(albumColumn) ?: "Unknown"
                    val artist = cursor.getString(artistColumn) ?: "Unknown"
                    val count = cursor.getInt(countColumn)
                    val artworkUri = ContentUris.withAppendedId(albumArtBaseUri, id)

                    albums.add(
                        Album(
                            id = id.toString(),
                            title = title,
                            artist = artist,
                            artworkUri = artworkUri.toString(),
                            trackCount = count
                        )
                    )
                }
            }
        } catch (e: Exception) {
            return@withContext emptyList()
        }

        albums
    }

    override suspend fun getTracksForAlbum(albumId: String): List<Track> =
        withContext(Dispatchers.IO) {
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
                MediaStore.Audio.Media.TRACK
            )

            val selection = "${MediaStore.Audio.Media.ALBUM_ID} = ? AND ${MediaStore.Audio.Media.IS_MUSIC} != 0"
            val selectionArgs = arrayOf(albumId)
            val sortOrder = "${MediaStore.Audio.Media.TRACK} ASC"

            try {
                context.contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
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

                    while (cursor.moveToNext()) {
                        val dataPath = cursor.getString(dataColumn)

                        if (dataPath == null || !File(dataPath).exists()) continue

                        val id = cursor.getLong(idColumn)
                        val title = cursor.getString(titleColumn) ?: "Unknown"
                        val artist = cursor.getString(artistColumn) ?: "Unknown"
                        val artistId = cursor.getLong(artistIdColumn).toString()
                        val album = cursor.getString(albumColumn) ?: ""
                        val currentAlbumId = cursor.getLong(albumIdColumn).toString()
                        val duration = cursor.getLong(durationColumn)
                        val folderPath = File(dataPath).parent

                        val uri = ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            id
                        )
                        val artworkUri = ContentUris.withAppendedId(
                            albumArtBaseUri,
                            currentAlbumId.toLongOrNull() ?: 0L
                        )

                        tracks.add(
                            Track(
                                id = id.toString(),
                                title = title,
                                artist = artist,
                                artistId = artistId,
                                album = album,
                                albumId = currentAlbumId,
                                durationMs = duration,
                                uri = uri.toString(),
                                artworkUri = artworkUri.toString(),
                                folderPath = folderPath
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                return@withContext emptyList()
            }

            val settings = settingsRepository.settings.value
            TrackFilter.mergeCovers(
                TrackFilter.apply(tracks, settings),
                settings,
                onlineCovers.value
            )
        }
}