package com.blueplayer.core.domain.repository

import com.blueplayer.core.domain.model.Album
import com.blueplayer.core.domain.model.Track

interface AlbumRepository {
    suspend fun getAlbums(): List<Album>
    suspend fun searchAlbums(query: String): List<Album>
    suspend fun getTracksForAlbum(albumId: String): List<Track>
}