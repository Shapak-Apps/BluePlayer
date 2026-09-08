package com.blueplayer.core.data.mediastore

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.blueplayer.core.domain.model.Genre
import com.blueplayer.core.domain.model.Track
import com.blueplayer.core.domain.repository.GenreRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MediaStoreGenreRepository(
    private val context: Context
) : GenreRepository {

    private val albumArtBaseUri = Uri.parse("content://media/external/audio/albumart")

    override suspend fun getGenres(): List<Genre> = withContext(Dispatchers.IO) {
        val genres = mutableListOf<Genre>()

        try {
            context.contentResolver.query(
                MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Audio.Genres._ID, MediaStore.Audio.Genres.NAME),
                null, null,
                "${MediaStore.Audio.Genres.NAME} ASC"
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Genres._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Genres.NAME)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn) ?: "Unknown"

                    val count = countMembers(id)
                    if (count > 0) {
                        genres.add(Genre(id.toString(), name, count))
                    }
                }
            }
        } catch (e: Exception) {
            return@withContext emptyList()
        }

        genres
    }

    override suspend fun getTracksForGenre(genreId: String): List<Track> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<Track>()

        val membersUri = Uri.parse(
            "${MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI}/$genreId/members"
        )

        try {
            context.contentResolver.query(
                membersUri,
                arrayOf(
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.ARTIST_ID,
                    MediaStore.Audio.Media.ALBUM,
                    MediaStore.Audio.Media.ALBUM_ID,
                    MediaStore.Audio.Media.DURATION,
                    MediaStore.Audio.Media.DATA
                ),
                null, null,
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
                    val albumId = cursor.getLong(albumIdColumn).toString()

                    tracks.add(
                        Track(
                            id = id.toString(),
                            title = cursor.getString(titleColumn) ?: "Unknown",
                            artist = cursor.getString(artistColumn) ?: "Unknown",
                            artistId = cursor.getLong(artistIdColumn).toString(),
                            album = cursor.getString(albumColumn) ?: "",
                            albumId = albumId,
                            durationMs = cursor.getLong(durationColumn),
                            uri = ContentUris.withAppendedId(
                                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                            ).toString(),
                            artworkUri = ContentUris.withAppendedId(
                                albumArtBaseUri, albumId.toLongOrNull() ?: 0L
                            ).toString(),
                            folderPath = File(dataPath).parent
                        )
                    )
                }
            }
        } catch (e: Exception) {
            return@withContext emptyList()
        }

        tracks
    }

    private fun countMembers(genreId: Long): Int {
        val membersUri = Uri.parse(
            "${MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI}/$genreId/members"
        )
        return try {
            context.contentResolver.query(
                membersUri,
                arrayOf(MediaStore.Audio.Media._ID),
                null, null, null
            )?.use { it.count } ?: 0
        } catch (e: Exception) {
            0
        }
    }
}