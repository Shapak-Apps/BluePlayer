package com.blueplayer.core.domain.repository

import com.blueplayer.core.domain.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface TrackRepository {
    val tracks: StateFlow<List<Track>>

    suspend fun getTracks(): List<Track>
    suspend fun searchTracks(query: String): List<Track>
    fun observeChanges(): Flow<Long>

    suspend fun rescan()
}