package com.blueplayer.core.domain.repository

import com.blueplayer.core.domain.model.Track
import kotlinx.coroutines.flow.Flow

interface TrackRepository {
    suspend fun getTracks(): List<Track>
    suspend fun searchTracks(query: String): List<Track>
    fun observeChanges(): Flow<Long>
}