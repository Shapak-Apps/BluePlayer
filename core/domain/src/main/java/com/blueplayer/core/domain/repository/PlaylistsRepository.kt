package com.blueplayer.core.domain.repository

import com.blueplayer.core.domain.model.Playlist
import com.blueplayer.core.domain.model.Track
import kotlinx.coroutines.flow.StateFlow

interface PlaylistsRepository {
    val playlists: StateFlow<List<Playlist>>
    suspend fun refresh()
    suspend fun createPlaylist(name: String)
    suspend fun renamePlaylist(id: Long, name: String)
    suspend fun deletePlaylist(id: Long)
    suspend fun getPlaylistTracks(playlistId: Long): List<Track>
    suspend fun addToPlaylist(playlistId: Long, track: Track)
    suspend fun removeFromPlaylist(playlistId: Long, trackId: String)
}