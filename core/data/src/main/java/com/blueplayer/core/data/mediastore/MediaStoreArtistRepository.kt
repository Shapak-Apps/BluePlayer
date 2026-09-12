package com.blueplayer.core.data.mediastore

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.blueplayer.core.domain.model.Artist
import com.blueplayer.core.domain.model.Track
import com.blueplayer.core.domain.repository.ArtistRepository
import com.blueplayer.core.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File

class MediaStoreArtistRepository(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val onlineCovers: StateFlow<Map<String, Any>>
) : ArtistRepository {

    private val albumArtBaseUri = Uri.parse("content://media/external/audio/albumart")

    override suspend fun getArtists(): List<Artist> = withContext(Dispatchers.IO) {
        val artists = mutableListOf<Artist>()

        val projection = arrayOf(
            MediaStore.Audio.Artists._ID,
            MediaStore.Audio.Artists.ARTIST,
            MediaStore.Audio.Artists.NUMBER_OF_ALBUMS,
            MediaStore.Audio.Artists.NUMBER_OF_TRACKS
        )

        val sortOrder = "${MediaStore.Audio.Artists.ARTIST} ASC"

        try {
            context.contentResolver.query(
                MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->

                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST)
                val albumCountColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.NUMBER_OF_ALBUMS)
                val trackCountColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.NUMBER_OF_TRACKS)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn) ?: "Unknown"
                    val albumCount = cursor.getInt(albumCountColumn)
                    val trackCount = cursor.getInt(trackCountColumn)

                    artists.add(
                        Artist(
                            id = id.toString(),
                            name = name,
                            albumCount = albumCount,
                            trackCount = trackCount
                        )
                    )
                }
            }
        } catch (e: Exception) {
            return@withContext emptyList()
        }

        artists
    }

    override suspend fun searchArtists(query: String): List<Artist> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()

        val artists = mutableListOf<Artist>()
        val selection = "${MediaStore.Audio.Artists.ARTIST} LIKE ?"
        val selectionArgs = arrayOf("%$query%")

        try {
            context.contentResolver.query(
                MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
                arrayOf(
                    MediaStore.Audio.Artists._ID,
                    MediaStore.Audio.Artists.ARTIST,
                    MediaStore.Audio.Artists.NUMBER_OF_ALBUMS,
                    MediaStore.Audio.Artists.NUMBER_OF_TRACKS
                ),
                selection,
                selectionArgs,
                "${MediaStore.Audio.Artists.ARTIST} ASC LIMIT 50"
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST)
                val albumCountColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.NUMBER_OF_ALBUMS)
                val trackCountColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.NUMBER_OF_TRACKS)

                while (cursor.moveToNext()) {
                    artists.add(
                        Artist(
                            id = cursor.getLong(idColumn).toString(),
                            name = cursor.getString(nameColumn) ?: "Unknown",
                            albumCount = cursor.getInt(albumCountColumn),
                            trackCount = cursor.getInt(trackCountColumn)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            return@withContext emptyList()
        }

        artists
    }

    override suspend fun getTracksForArtist(artistId: String): List<Track> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<Track>()
        val selection = "${MediaStore.Audio.Media.ARTIST_ID} = ? AND ${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val selectionArgs = arrayOf(artistId)

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ARTIST_ID,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA
        )

        try {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${MediaStore.Audio.Media.TITLE} ASC"
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
                    val curArtistId = cursor.getLong(artistIdColumn).toString()
                    val album = cursor.getString(albumColumn) ?: ""
                    val albumId = cursor.getLong(albumIdColumn).toString()
                    val duration = cursor.getLong(durationColumn)
                    val folderPath = File(dataPath).parent

                    val uri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
                    val artworkUri = ContentUris.withAppendedId(albumArtBaseUri, albumId.toLongOrNull() ?: 0L)

                    tracks.add(
                        Track(
                            id = id.toString(),
                            title = title,
                            artist = artist,
                            artistId = curArtistId,
                            album = album,
                            albumId = albumId,
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