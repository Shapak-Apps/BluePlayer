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

sealed interface AlbumsUiState {
    data object Loading : AlbumsUiState
    data class Success(val albums: List<Album>) : AlbumsUiState
    data class Error(val message: String) : AlbumsUiState
}

class AlbumsViewModel(
    private val albumRepository: AlbumRepository,
    private val playerController: PlayerController
) : ViewModel() {

    private val _uiState = MutableStateFlow<AlbumsUiState>(AlbumsUiState.Loading)
    val uiState: StateFlow<AlbumsUiState> = _uiState.asStateFlow()

    init {
        loadAlbums()
    }

    fun refresh() {
        loadAlbums()
    }

    fun playAlbum(albumId: String) {
        viewModelScope.launch {
            try {
                val tracks = albumRepository.getTracksForAlbum(albumId)
                if (tracks.isNotEmpty()) {
                    playerController.playTracks(tracks, 0)
                }
            } catch (e: Exception) {
            }
        }
    }

    private fun loadAlbums() {
        viewModelScope.launch {
            _uiState.value = AlbumsUiState.Loading

            try {
                val albums = albumRepository.getAlbums()
                _uiState.value = AlbumsUiState.Success(albums)
            } catch (e: Exception) {
                _uiState.value = AlbumsUiState.Error("Не удалось загрузить альбомы")
            }
        }
    }
}

class AlbumsViewModelFactory(
    private val albumRepository: AlbumRepository,
    private val playerController: PlayerController
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AlbumsViewModel(albumRepository, playerController) as T
    }
}