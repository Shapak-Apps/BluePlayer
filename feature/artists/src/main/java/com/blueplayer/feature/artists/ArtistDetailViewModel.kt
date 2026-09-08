package com.blueplayer.feature.artists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.blueplayer.core.domain.model.Artist
import com.blueplayer.core.domain.model.Track
import com.blueplayer.core.domain.player.PlayerController
import com.blueplayer.core.domain.repository.ArtistRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ArtistDetailState(
    val artist: Artist? = null,
    val tracks: List<Track> = emptyList(),
    val isLoading: Boolean = true
)

class ArtistDetailViewModel(
    private val artistId: String,
    private val artistRepository: ArtistRepository,
    private val playerController: PlayerController
) : ViewModel() {

    private val _state = MutableStateFlow(ArtistDetailState())
    val state: StateFlow<ArtistDetailState> = _state.asStateFlow()

    init {
        load()
    }

    fun playTrack(index: Int) {
        val tracks = _state.value.tracks
        if (tracks.isNotEmpty()) playerController.playTracks(tracks, index)
    }

    fun playAll() = playTrack(0)

    private fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                val artists = artistRepository.getArtists()
                val artist = artists.find { it.id == artistId }
                val tracks = artistRepository.getTracksForArtist(artistId)
                _state.value = ArtistDetailState(artist = artist, tracks = tracks, isLoading = false)
            } catch (_: Exception) {
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }
}

class ArtistDetailViewModelFactory(
    private val artistId: String,
    private val artistRepository: ArtistRepository,
    private val playerController: PlayerController
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ArtistDetailViewModel(artistId, artistRepository, playerController) as T
    }
}