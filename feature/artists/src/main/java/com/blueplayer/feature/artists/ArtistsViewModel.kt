package com.blueplayer.feature.artists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.blueplayer.core.domain.model.Artist
import com.blueplayer.core.domain.player.PlayerController
import com.blueplayer.core.domain.repository.ArtistRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ArtistsUiState {
    data object Loading : ArtistsUiState
    data class Success(val artists: List<Artist>) : ArtistsUiState
    data class Error(val message: String) : ArtistsUiState
}

class ArtistsViewModel(
    private val artistRepository: ArtistRepository,
    private val playerController: PlayerController
) : ViewModel() {

    private val _uiState = MutableStateFlow<ArtistsUiState>(ArtistsUiState.Loading)
    val uiState: StateFlow<ArtistsUiState> = _uiState.asStateFlow()

    init {
        loadArtists()
    }

    fun refresh() = loadArtists()

    fun playArtist(artistId: String) {
        viewModelScope.launch {
            val tracks = artistRepository.getTracksForArtist(artistId)
            if (tracks.isNotEmpty()) playerController.playTracks(tracks, 0)
        }
    }

    private fun loadArtists() {
        viewModelScope.launch {
            _uiState.value = ArtistsUiState.Loading
            try {
                _uiState.value = ArtistsUiState.Success(artistRepository.getArtists())
            } catch (e: Exception) {
                _uiState.value = ArtistsUiState.Error("Не удалось загрузить исполнителей")
            }
        }
    }
}

class ArtistsViewModelFactory(
    private val artistRepository: ArtistRepository,
    private val playerController: PlayerController
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ArtistsViewModel(artistRepository, playerController) as T
}