package com.blueplayer.core.database

import android.content.ContentValues
import android.content.Context
import com.blueplayer.core.domain.model.Playlist
import com.blueplayer.core.domain.model.Track
import com.blueplayer.core.domain.repository.PlaylistsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlaylistsRepositoryImpl(context: Context) : PlaylistsRepository {

    private val helper = MusicDatabase(context)

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    override val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    init {
        CoroutineScope(Dispatchers.IO).launch { refresh() }
    }

    override suspend fun refresh() = withContext(Dispatchers.IO) {
        val list = mutableListOf<Playlist>()

        helper.readableDatabase.rawQuery(
            """
            SELECT p.id, p.name,
                   (SELECT COUNT(*) FROM ${MusicDatabase.TABLE_PLAYLIST_TRACKS} t
                    WHERE t.playlist_id = p.id) AS cnt
            FROM ${MusicDatabase.TABLE_PLAYLISTS} p
            ORDER BY p.created_at DESC
            """.trimIndent(),
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(
                    Playlist(
                        id = cursor.getLong(0),
                        name = cursor.getString(1) ?: "",
                        trackCount = cursor.getInt(2)
                    )
                )
            }
        }

        _playlists.value = list
    }

    override suspend fun createPlaylist(name: String) = withContext(Dispatchers.IO) {
        val cv = ContentValues().apply {
            put("name", name)
            put("created_at", System.currentTimeMillis())
        }
        helper.writableDatabase.insert(MusicDatabase.TABLE_PLAYLISTS, null, cv)
        refresh()
    }

    override suspend fun renamePlaylist(id: Long, name: String) = withContext(Dispatchers.IO) {
        val cv = ContentValues().apply { put("name", name) }
        helper.writableDatabase.update(
            MusicDatabase.TABLE_PLAYLISTS,
            cv,
            "id = ?",
            arrayOf(id.toString())
        )
        refresh()
    }

    override suspend fun deletePlaylist(id: Long) = withContext(Dispatchers.IO) {
        val db = helper.writableDatabase
        db.delete(MusicDatabase.TABLE_PLAYLIST_TRACKS, "playlist_id = ?", arrayOf(id.toString()))
        db.delete(MusicDatabase.TABLE_PLAYLISTS, "id = ?", arrayOf(id.toString()))
        refresh()
    }

    override suspend fun getPlaylistTracks(playlistId: Long): List<Track> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Track>()

        helper.readableDatabase.query(
            MusicDatabase.TABLE_PLAYLIST_TRACKS,
            null,
            "playlist_id = ?",
            arrayOf(playlistId.toString()),
            null, null,
            "position ASC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(cursor.toTrack())
            }
        }

        list
    }

    override suspend fun addToPlaylist(playlistId: Long, track: Track) = withContext(Dispatchers.IO) {
        val position = getPlaylistTracks(playlistId).size
        helper.writableDatabase.insert(
            MusicDatabase.TABLE_PLAYLIST_TRACKS,
            null,
            track.toPlaylistContentValues(playlistId, position)
        )
        refresh()
    }

    override suspend fun removeFromPlaylist(playlistId: Long, trackId: String) = withContext(Dispatchers.IO) {
        helper.writableDatabase.delete(
            MusicDatabase.TABLE_PLAYLIST_TRACKS,
            "playlist_id = ? AND track_id = ?",
            arrayOf(playlistId.toString(), trackId)
        )
        refresh()
    }
}