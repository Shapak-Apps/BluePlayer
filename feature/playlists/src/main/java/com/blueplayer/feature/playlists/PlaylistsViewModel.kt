package com.blueplayer.feature.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.blueplayer.core.domain.model.Playlist
import com.blueplayer.core.domain.model.Track
import com.blueplayer.core.domain.player.PlayerController
import com.blueplayer.core.domain.repository.PlaylistsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PlaylistDetailState(
    val playlist: Playlist? = null,
    val tracks: List<Track> = emptyList()
)

class PlaylistsViewModel(
    private val playlistsRepository: PlaylistsRepository,
    private val playerController: PlayerController
) : ViewModel() {

    val playlists = playlistsRepository.playlists

    private val _detail = MutableStateFlow(PlaylistDetailState())
    val detail: StateFlow<PlaylistDetailState> = _detail.asStateFlow()

    fun openPlaylist(playlist: Playlist) {
        viewModelScope.launch {
            val tracks = playlistsRepository.getPlaylistTracks(playlist.id)
            _detail.value = PlaylistDetailState(playlist, tracks)
        }
    }

    fun closePlaylist() {
        _detail.value = PlaylistDetailState()
    }

    fun create(name: String) {
        viewModelScope.launch { playlistsRepository.createPlaylist(name) }
    }

    fun rename(id: Long, name: String) {
        viewModelScope.launch { playlistsRepository.renamePlaylist(id, name) }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            playlistsRepository.deletePlaylist(id)
            if (_detail.value.playlist?.id == id) closePlaylist()
        }
    }

    fun removeTrack(playlistId: Long, trackId: String) {
        viewModelScope.launch {
            playlistsRepository.removeFromPlaylist(playlistId, trackId)
            openPlaylist(Playlist(playlistId, "", 0))
        }
    }

    fun playAll() {
        val tracks = _detail.value.tracks
        if (tracks.isNotEmpty()) playerController.playTracks(tracks, 0)
    }

    fun playTrack(index: Int) {
        val tracks = _detail.value.tracks
        if (tracks.isNotEmpty()) playerController.playTracks(tracks, index)
    }
}

class PlaylistsViewModelFactory(
    private val playlistsRepository: PlaylistsRepository,
    private val playerController: PlayerController
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        PlaylistsViewModel(playlistsRepository, playerController) as T
}