package com.blueplayer.core.domain.repository

import com.blueplayer.core.domain.model.Track
import kotlinx.coroutines.flow.StateFlow

interface FavoritesRepository {
    val favorites: StateFlow<List<Track>>
    suspend fun isFavorite(trackId: String): Boolean
    suspend fun toggleFavorite(track: Track)
    suspend fun refresh()
}