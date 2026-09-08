package com.blueplayer.feature.genres

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.blueplayer.core.domain.model.Genre
import com.blueplayer.core.domain.model.Track
import com.blueplayer.core.domain.player.PlayerController
import com.blueplayer.core.domain.repository.GenreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface GenresUiState {
    data object Loading : GenresUiState
    data class Success(val genres: List<Genre>) : GenresUiState
    data class Error(val message: String) : GenresUiState
}

data class GenreDetailState(
    val genre: Genre? = null,
    val tracks: List<Track> = emptyList(),
    val isLoading: Boolean = true
)

class GenresViewModel(
    private val genreRepository: GenreRepository,
    private val playerController: PlayerController
) : ViewModel() {

    private val _uiState = MutableStateFlow<GenresUiState>(GenresUiState.Loading)
    val uiState: StateFlow<GenresUiState> = _uiState.asStateFlow()

    private val _detail = MutableStateFlow(GenreDetailState())
    val detail: StateFlow<GenreDetailState> = _detail.asStateFlow()

    init { load() }

    fun refresh() = load()

    fun openGenre(genre: Genre) {
        viewModelScope.launch {
            _detail.value = GenreDetailState(genre = genre, isLoading = true)
            val tracks = genreRepository.getTracksForGenre(genre.id)
            _detail.value = GenreDetailState(genre = genre, tracks = tracks, isLoading = false)
        }
    }

    fun closeGenre() {
        _detail.value = GenreDetailState()
    }

    fun playAll() {
        val tracks = _detail.value.tracks
        if (tracks.isNotEmpty()) playerController.playTracks(tracks, 0)
    }

    fun playTrack(index: Int) {
        val tracks = _detail.value.tracks
        if (tracks.isNotEmpty()) playerController.playTracks(tracks, index)
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.value = GenresUiState.Loading
            try {
                _uiState.value = GenresUiState.Success(genreRepository.getGenres())
            } catch (e: Exception) {
                _uiState.value = GenresUiState.Error("Error")
            }
        }
    }
}

class GenresViewModelFactory(
    private val genreRepository: GenreRepository,
    private val playerController: PlayerController
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        GenresViewModel(genreRepository, playerController) as T
}