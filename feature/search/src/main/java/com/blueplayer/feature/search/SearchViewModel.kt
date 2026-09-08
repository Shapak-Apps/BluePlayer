package com.blueplayer.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.blueplayer.core.domain.model.Album
import com.blueplayer.core.domain.model.Artist
import com.blueplayer.core.domain.model.Track
import com.blueplayer.core.domain.player.PlayerController
import com.blueplayer.core.domain.repository.AlbumRepository
import com.blueplayer.core.domain.repository.ArtistRepository
import com.blueplayer.core.domain.repository.TrackRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

data class SearchState(
    val query: String = "",
    val tracks: List<Track> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val isSearching: Boolean = false
)

@OptIn(FlowPreview::class)
class SearchViewModel(
    private val trackRepository: TrackRepository,
    private val albumRepository: AlbumRepository,
    private val artistRepository: ArtistRepository,
    private val playerController: PlayerController
) : ViewModel() {

    private val _state = MutableStateFlow(SearchState())
    val state: StateFlow<SearchState> = _state.asStateFlow()

    private val _queryFlow = MutableStateFlow("")

    init {
        _queryFlow
            .debounce(300L)
            .distinctUntilChanged()
            .onEach { query ->
                if (query.length < 2) {
                    _state.value = SearchState(query = query)
                    return@onEach
                }

                _state.value = _state.value.copy(isSearching = true, query = query)

                val tracks = trackRepository.searchTracks(query)
                val albums = albumRepository.searchAlbums(query)
                val artists = artistRepository.searchArtists(query)

                _state.value = SearchState(
                    query = query,
                    tracks = tracks,
                    albums = albums,
                    artists = artists,
                    isSearching = false
                )
            }
            .launchIn(viewModelScope)
    }

    fun onQueryChange(query: String) {
        _queryFlow.value = query
        _state.value = _state.value.copy(query = query)
    }

    fun playTracks(tracks: List<Track>, startIndex: Int) {
        playerController.playTracks(tracks, startIndex)
    }
}

class SearchViewModelFactory(
    private val trackRepository: TrackRepository,
    private val albumRepository: AlbumRepository,
    private val artistRepository: ArtistRepository,
    private val playerController: PlayerController
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        SearchViewModel(trackRepository, albumRepository, artistRepository, playerController) as T
}