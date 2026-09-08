package com.blueplayer.core.domain.repository

import com.blueplayer.core.domain.model.Artist
import com.blueplayer.core.domain.model.Track

interface ArtistRepository {
    suspend fun getArtists(): List<Artist>
    suspend fun searchArtists(query: String): List<Artist>
    suspend fun getTracksForArtist(artistId: String): List<Track>
}