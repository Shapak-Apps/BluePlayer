package com.blueplayer.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.blueplayer.core.domain.model.Track
import com.blueplayer.core.domain.player.PlayerController
import com.blueplayer.core.domain.player.PlayerState
import com.blueplayer.core.domain.repository.TrackRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

sealed interface LibraryUiState {
    data object Loading : LibraryUiState
    data class Success(val tracks: List<Track>) : LibraryUiState
    data class Error(val message: String) : LibraryUiState
}

class LibraryViewModel(
    private val trackRepository: TrackRepository,
    private val playerController: PlayerController
) : ViewModel() {

    private val _uiState = MutableStateFlow<LibraryUiState>(LibraryUiState.Loading)
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    val playerState: StateFlow<PlayerState> = playerController.state
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PlayerState()
        )

    init {
        loadTracks()
        viewModelScope.launch {
            trackRepository.observeChanges().collect {
                loadTracks()
            }
        }
    }

    fun refresh() {
        loadTracks()
    }

    fun play(index: Int) {
        val state = uiState.value as? LibraryUiState.Success ?: return
        playerController.playTracks(state.tracks, index)
    }

    private fun loadTracks() {
        viewModelScope.launch {
            _uiState.value = LibraryUiState.Loading

            try {
                val tracks = trackRepository.getTracks()
                _uiState.value = LibraryUiState.Success(tracks)
            } catch (e: Exception) {
                _uiState.value = LibraryUiState.Error("Не удалось загрузить музыку")
            }
        }
    }
}

class LibraryViewModelFactory(
    private val trackRepository: TrackRepository,
    private val playerController: PlayerController
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return LibraryViewModel(trackRepository, playerController) as T
    }
}