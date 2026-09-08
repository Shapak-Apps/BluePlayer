package com.blueplayer.core.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.blueplayer.core.domain.model.Bookmark
import com.blueplayer.core.domain.model.Track
import com.blueplayer.core.domain.repository.BookmarksRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BookmarksRepositoryImpl(context: Context) : BookmarksRepository {

    private val helper = BookmarksDbHelper(context)

    private val _bookmarks = MutableStateFlow<List<Bookmark>>(emptyList())
    override val bookmarks: StateFlow<List<Bookmark>> = _bookmarks.asStateFlow()

    init {
        CoroutineScope(Dispatchers.IO).launch { refresh() }
    }

    override suspend fun refresh() = withContext(Dispatchers.IO) {
        val list = mutableListOf<Bookmark>()
        helper.readableDatabase.query(
            "bookmarks", null, null, null, null, null, "added_at DESC"
        ).use { c ->
            while (c.moveToNext()) {
                list.add(
                    Bookmark(
                        id = c.getLong(c.getColumnIndexOrThrow("id")),
                        track = Track(
                            id = c.getString(c.getColumnIndexOrThrow("track_id")),
                            title = c.getString(c.getColumnIndexOrThrow("title")),
                            artist = c.getString(c.getColumnIndexOrThrow("artist")),
                            artistId = c.getString(c.getColumnIndexOrThrow("artist_id")),
                            album = c.getString(c.getColumnIndexOrThrow("album")),
                            albumId = c.getString(c.getColumnIndexOrThrow("album_id")),
                            durationMs = c.getLong(c.getColumnIndexOrThrow("duration")),
                            uri = c.getString(c.getColumnIndexOrThrow("uri")),
                            artworkUri = c.getString(c.getColumnIndexOrThrow("artwork_uri")),
                            folderPath = null
                        ),
                        positionMs = c.getLong(c.getColumnIndexOrThrow("position")),
                        addedAt = c.getLong(c.getColumnIndexOrThrow("added_at"))
                    )
                )
            }
        }
        _bookmarks.value = list
    }

    override suspend fun add(track: Track, positionMs: Long) = withContext(Dispatchers.IO) {
        val cv = ContentValues().apply {
            put("track_id", track.id)
            put("title", track.title)
            put("artist", track.artist)
            put("artist_id", track.artistId)
            put("album", track.album)
            put("album_id", track.albumId)
            put("duration", track.durationMs)
            put("uri", track.uri)
            put("artwork_uri", track.artworkUri)
            put("position", positionMs)
            put("added_at", System.currentTimeMillis())
        }
        helper.writableDatabase.insert("bookmarks", null, cv)
        refresh()
    }

    override suspend fun remove(id: Long) = withContext(Dispatchers.IO) {
        helper.writableDatabase.delete("bookmarks", "id = ?", arrayOf(id.toString()))
        refresh()
    }

    private class BookmarksDbHelper(context: Context) :
        SQLiteOpenHelper(context, "bookmarks.db", null, 1) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE bookmarks (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    track_id TEXT NOT NULL,
                    title TEXT NOT NULL,
                    artist TEXT NOT NULL,
                    artist_id TEXT,
                    album TEXT,
                    album_id TEXT,
                    duration INTEGER NOT NULL DEFAULT 0,
                    uri TEXT NOT NULL,
                    artwork_uri TEXT,
                    position INTEGER NOT NULL DEFAULT 0,
                    added_at INTEGER NOT NULL DEFAULT 0
                )
                """.trimIndent()
            )
        }
        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
    }
}