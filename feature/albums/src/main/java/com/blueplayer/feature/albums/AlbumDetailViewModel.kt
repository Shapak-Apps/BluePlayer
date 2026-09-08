package com.blueplayer.feature.albums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.blueplayer.core.domain.model.Album
import com.blueplayer.core.domain.model.Track
import com.blueplayer.core.domain.player.PlayerController
import com.blueplayer.core.domain.repository.AlbumRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AlbumDetailState(
    val album: Album? = null,
    val tracks: List<Track> = emptyList(),
    val isLoading: Boolean = true
)

class AlbumDetailViewModel(
    private val albumId: String,
    private val albumRepository: AlbumRepository,
    private val playerController: PlayerController
) : ViewModel() {

    private val _state = MutableStateFlow(AlbumDetailState())
    val state: StateFlow<AlbumDetailState> = _state.asStateFlow()

    init {
        loadAlbumDetails()
    }

    fun playTrack(index: Int) {
        val tracks = _state.value.tracks
        if (tracks.isNotEmpty()) {
            playerController.playTracks(tracks, index)
        }
    }

    fun playAll() {
        playTrack(0)
    }

    private fun loadAlbumDetails() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            try {
                val albums = albumRepository.getAlbums()
                val album = albums.find { it.id == albumId }

                val tracks = albumRepository.getTracksForAlbum(albumId)

                _state.value = AlbumDetailState(
                    album = album,
                    tracks = tracks,
                    isLoading = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }
}

class AlbumDetailViewModelFactory(
    private val albumId: String,
    private val albumRepository: AlbumRepository,
    private val playerController: PlayerController
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AlbumDetailViewModel(albumId, albumRepository, playerController) as T
    }
}