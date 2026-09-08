package com.blueplayer.core.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.blueplayer.core.domain.model.Track

class MusicDatabase(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "blueplayer.db"
        private const val DATABASE_VERSION = 1

        const val TABLE_FAVORITES = "favorites"
        const val TABLE_PLAYLISTS = "playlists"
        const val TABLE_PLAYLIST_TRACKS = "playlist_tracks"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_FAVORITES (
                track_id TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                artist TEXT NOT NULL,
                artist_id TEXT,
                album TEXT,
                album_id TEXT,
                duration INTEGER NOT NULL DEFAULT 0,
                uri TEXT NOT NULL,
                artwork_uri TEXT,
                added_at INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_PLAYLISTS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                created_at INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_PLAYLIST_TRACKS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                playlist_id INTEGER NOT NULL,
                track_id TEXT NOT NULL,
                title TEXT NOT NULL,
                artist TEXT NOT NULL,
                artist_id TEXT,
                album TEXT,
                album_id TEXT,
                duration INTEGER NOT NULL DEFAULT 0,
                uri TEXT NOT NULL,
                artwork_uri TEXT,
                position INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    }
}

fun Cursor.toTrack(): Track {
    return Track(
        id = getString(getColumnIndexOrThrow("track_id")),
        title = getString(getColumnIndexOrThrow("title")) ?: "Unknown",
        artist = getString(getColumnIndexOrThrow("artist")) ?: "Unknown",
        artistId = getString(getColumnIndexOrThrow("artist_id")) ?: "",
        album = getString(getColumnIndexOrThrow("album")) ?: "",
        albumId = getString(getColumnIndexOrThrow("album_id")) ?: "",
        durationMs = getLong(getColumnIndexOrThrow("duration")),
        uri = getString(getColumnIndexOrThrow("uri")),
        artworkUri = getString(getColumnIndexOrThrow("artwork_uri")),
        folderPath = null
    )
}

fun Track.toContentValues(): ContentValues {
    return ContentValues().apply {
        put("track_id", id)
        put("title", title)
        put("artist", artist)
        put("artist_id", artistId)
        put("album", album)
        put("album_id", albumId)
        put("duration", durationMs)
        put("uri", uri)
        put("artwork_uri", artworkUri)
        put("added_at", System.currentTimeMillis())
    }
}

fun Track.toPlaylistContentValues(playlistId: Long, position: Int): ContentValues {
    return ContentValues().apply {
        put("playlist_id", playlistId)
        put("track_id", id)
        put("title", title)
        put("artist", artist)
        put("artist_id", artistId)
        put("album", album)
        put("album_id", albumId)
        put("duration", durationMs)
        put("uri", uri)
        put("artwork_uri", artworkUri)
        put("position", position)
    }
}