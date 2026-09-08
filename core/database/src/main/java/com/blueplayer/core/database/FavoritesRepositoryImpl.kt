package com.blueplayer.core.database

import android.content.Context
import com.blueplayer.core.domain.model.Track
import com.blueplayer.core.domain.repository.FavoritesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FavoritesRepositoryImpl(context: Context) : FavoritesRepository {

    private val helper = MusicDatabase(context)

    private val _favorites = MutableStateFlow<List<Track>>(emptyList())
    override val favorites: StateFlow<List<Track>> = _favorites.asStateFlow()

    init {
        CoroutineScope(Dispatchers.IO).launch { refresh() }
    }

    override suspend fun refresh() = withContext(Dispatchers.IO) {
        val list = mutableListOf<Track>()

        helper.readableDatabase.query(
            MusicDatabase.TABLE_FAVORITES,
            null, null, null, null, null,
            "added_at DESC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(cursor.toTrack())
            }
        }

        _favorites.value = list
    }

    override suspend fun isFavorite(trackId: String): Boolean = withContext(Dispatchers.IO) {
        helper.readableDatabase.query(
            MusicDatabase.TABLE_FAVORITES,
            arrayOf("track_id"),
            "track_id = ?",
            arrayOf(trackId),
            null, null, null
        ).use { it.count > 0 }
    }

    override suspend fun toggleFavorite(track: Track) = withContext(Dispatchers.IO) {
        if (isFavorite(track.id)) {
            helper.writableDatabase.delete(
                MusicDatabase.TABLE_FAVORITES,
                "track_id = ?",
                arrayOf(track.id)
            )
        } else {
            helper.writableDatabase.insert(
                MusicDatabase.TABLE_FAVORITES,
                null,
                track.toContentValues()
            )
        }
        refresh()
    }
}