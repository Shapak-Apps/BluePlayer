package com.blueplayer.core.data.mediastore

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.blueplayer.core.domain.model.Folder
import com.blueplayer.core.domain.model.Track
import com.blueplayer.core.domain.repository.FolderRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MediaStoreFolderRepository(
    private val context: Context
) : FolderRepository {

    override suspend fun getFolders(): List<Folder> = withContext(Dispatchers.IO) {
        val tracks = fetchAllTracksInternal()
        val folderMap = linkedMapOf<String, MutableList<Track>>()

        tracks.forEach { track ->
            val path = track.folderPath ?: return@forEach
            folderMap.getOrPut(path) { mutableListOf() }.add(track)
        }

        folderMap.map { (path, folderTracks) ->
            Folder(
                id = path.hashCode().toString(),
                name = File(path).name,
                path = path,
                trackCount = folderTracks.size
            )
        }.sortedBy { it.name.lowercase() }
    }

    override suspend fun getTracksForFolder(folderPath: String): List<Track> = withContext(Dispatchers.IO) {
        fetchAllTracksInternal().filter { it.folderPath == folderPath }
    }

    private fun fetchAllTracksInternal(): List<Track> {
        val tracks = mutableListOf<Track>()

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

        val albumArtBaseUri = Uri.parse("content://media/external/audio/albumart")

        try {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                "${MediaStore.Audio.Media.IS_MUSIC} != 0",
                null,
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
                    val artistId = cursor.getLong(artistIdColumn).toString()
                    val album = cursor.getString(albumColumn) ?: ""
                    val albumId = cursor.getLong(albumIdColumn).toString()
                    val duration = cursor.getLong(durationColumn)
                    val folderPath = File(dataPath).parent

                    val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    val artworkUri = ContentUris.withAppendedId(albumArtBaseUri, albumId.toLongOrNull() ?: 0L)

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
                            folderPath = folderPath
                        )
                    )
                }
            }
        } catch (e: Exception) {
            return emptyList()
        }

        return tracks
    }
}