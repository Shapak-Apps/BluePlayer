package com.blueplayer.feature.folders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.blueplayer.core.domain.model.Folder
import com.blueplayer.core.domain.model.Track
import com.blueplayer.core.domain.player.PlayerController
import com.blueplayer.core.domain.repository.FolderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FoldersState(
    val folders: List<Folder> = emptyList(),
    val selectedFolder: Folder? = null,
    val selectedFolderTracks: List<Track> = emptyList(),
    val isLoading: Boolean = true
)

class FoldersViewModel(
    private val folderRepository: FolderRepository,
    private val playerController: PlayerController
) : ViewModel() {

    private val _state = MutableStateFlow(FoldersState())
    val state: StateFlow<FoldersState> = _state.asStateFlow()

    init {
        loadFolders()
    }

    fun refresh() {
        if (_state.value.selectedFolder != null) {
            loadFolderTracks(_state.value.selectedFolder!!)
        } else {
            loadFolders()
        }
    }

    fun selectFolder(folder: Folder) {
        _state.value = _state.value.copy(selectedFolder = folder, isLoading = true)
        loadFolderTracks(folder)
    }

    fun goBack() {
        _state.value = FoldersState(isLoading = false, folders = _state.value.folders)
    }

    fun playTrack(index: Int) {
        val tracks = _state.value.selectedFolderTracks
        if (tracks.isNotEmpty()) playerController.playTracks(tracks, index)
    }

    private fun loadFolders() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                val folders = folderRepository.getFolders()
                _state.value = FoldersState(folders = folders, isLoading = false)
            } catch (_: Exception) {
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }

    private fun loadFolderTracks(folder: Folder) {
        viewModelScope.launch {
            try {
                val tracks = folderRepository.getTracksForFolder(folder.path)
                _state.value = _state.value.copy(selectedFolderTracks = tracks, isLoading = false)
            } catch (_: Exception) {
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }
}

class FoldersViewModelFactory(
    private val folderRepository: FolderRepository,
    private val playerController: PlayerController
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return FoldersViewModel(folderRepository, playerController) as T
    }
}