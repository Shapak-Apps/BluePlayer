package com.blueplayer.core.domain.repository

import com.blueplayer.core.domain.model.Genre
import com.blueplayer.core.domain.model.Track

interface GenreRepository {
    suspend fun getGenres(): List<Genre>
    suspend fun getTracksForGenre(genreId: String): List<Track>
}