package com.blueplayer.core.domain.repository

import com.blueplayer.core.domain.model.Bookmark
import com.blueplayer.core.domain.model.Track
import kotlinx.coroutines.flow.StateFlow

interface BookmarksRepository {
    val bookmarks: StateFlow<List<Bookmark>>
    suspend fun add(track: Track, positionMs: Long)
    suspend fun remove(id: Long)
    suspend fun refresh()
}