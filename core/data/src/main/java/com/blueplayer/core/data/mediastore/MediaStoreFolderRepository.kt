package com.blueplayer.core.data.mediastore

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import com.blueplayer.core.domain.model.Folder
import com.blueplayer.core.domain.model.Track
import com.blueplayer.core.domain.repository.FolderRepository
import com.blueplayer.core.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File

class MediaStoreFolderRepository(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val onlineCovers: StateFlow<Map<String, Any>>
) : FolderRepository {

    private val albumArtBaseUri = Uri.parse("content://media/external/audio/albumart")

    private val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ARTIST_ID,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.ALBUM_ID,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.DATA
    )

    /**
     * Folder list with counts.
     *
     * Optimization: cover merging is skipped — the folder list screen
     * only needs names, paths and counts, not artwork models.
     */
    override suspend fun getFolders(): List<Folder> = withContext(Dispatchers.IO) {
        val settings = settingsRepository.settings.value
        val tracks = TrackFilter.apply(fetchAllTracksInternal(), settings)
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

    /**
     * Tracks inside one folder.
     *
     * Optimization: a targeted LIKE query scans ONLY this folder's rows
     * in MediaStore instead of re-reading the whole library.
     * Covers are merged here because the folder screen shows artwork.
     */
    override suspend fun getTracksForFolder(folderPath: String): List<Track> = withContext(Dispatchers.IO) {
        val settings = settingsRepository.settings.value
        val raw = fetchTracksInFolder(folderPath)
        TrackFilter.mergeCovers(
            TrackFilter.apply(raw, settings),
            settings,
            onlineCovers.value
        )
    }

    /**
     * Single-folder query using DATA LIKE with escaped wildcards.
     */
    private fun fetchTracksInFolder(folderPath: String): List<Track> {
        // Escape LIKE wildcards so unusual folder names match literally
        val escaped = folderPath
            .replace("!", "!!")
            .replace("%", "!%")
            .replace("_", "!_")

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND " +
                "${MediaStore.Audio.Media.DATA} LIKE ? ESCAPE '!'"
        val selectionArgs = arrayOf("$escaped/%")

        return try {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${MediaStore.Audio.Media.TITLE} ASC"
            )?.use { cursor -> mapCursor(cursor) } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun fetchAllTracksInternal(): List<Track> {
        return try {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                "${MediaStore.Audio.Media.IS_MUSIC} != 0",
                null,
                "${MediaStore.Audio.Media.TITLE} ASC"
            )?.use { cursor -> mapCursor(cursor) } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Shared cursor -> Track mapping used by both query paths.
     */
    private fun mapCursor(cursor: Cursor): List<Track> {
        val tracks = mutableListOf<Track>()

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

        return tracks
    }
}