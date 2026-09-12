package com.blueplayer.core.data.prefs

import android.content.Context
import android.content.SharedPreferences
import com.blueplayer.core.domain.model.Track
import org.json.JSONArray
import org.json.JSONObject

data class SavedQueue(
    val tracks: List<Track>,
    val index: Int,
    val positionMs: Long,
    val wasPlaying: Boolean
)

class QueuePersistenceStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("queue_persistence", Context.MODE_PRIVATE)

    fun saveQueue(tracks: List<Track>, index: Int, wasPlaying: Boolean) {
        val array = JSONArray()
        tracks.forEach { t ->
            array.put(
                JSONObject()
                    .put("id", t.id)
                    .put("title", t.title)
                    .put("artist", t.artist)
                    .put("artist_id", t.artistId)
                    .put("album", t.album)
                    .put("album_id", t.albumId)
                    .put("duration", t.durationMs)
                    .put("uri", t.uri)
                    .put("artwork_uri", t.artworkUri ?: JSONObject.NULL)
                    .put("folder_path", t.folderPath ?: JSONObject.NULL)
            )
        }
        prefs.edit()
            .putString("queue_json", array.toString())
            .putInt("queue_index", index)
            .putBoolean("was_playing", wasPlaying)
            .apply()
    }

    fun savePosition(positionMs: Long, index: Int) {
        prefs.edit()
            .putLong("position_ms", positionMs)
            .putInt("queue_index", index)
            .apply()
    }

    fun load(): SavedQueue? {
        val json = prefs.getString("queue_json", null) ?: return null
        return runCatching {
            val array = JSONArray(json)
            val tracks = mutableListOf<Track>()
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                tracks.add(
                    Track(
                        id = o.getString("id"),
                        title = o.optString("title", "Unknown"),
                        artist = o.optString("artist", "Unknown"),
                        artistId = o.optString("artist_id", ""),
                        album = o.optString("album", ""),
                        albumId = o.optString("album_id", ""),
                        durationMs = o.optLong("duration", 0L),
                        uri = o.getString("uri"),
                        artworkUri = if (o.isNull("artwork_uri")) null else o.getString("artwork_uri"),
                        folderPath = if (o.isNull("folder_path")) null else o.getString("folder_path")
                    )
                )
            }
            if (tracks.isEmpty()) null
            else SavedQueue(
                tracks = tracks,
                index = prefs.getInt("queue_index", 0).coerceIn(0, tracks.lastIndex),
                positionMs = prefs.getLong("position_ms", 0L),
                wasPlaying = prefs.getBoolean("was_playing", false)
            )
        }.getOrNull()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}