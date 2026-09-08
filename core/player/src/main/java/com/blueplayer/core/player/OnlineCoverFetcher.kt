package com.blueplayer.core.player

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class OnlineCoverFetcher(private val context: Context) {

    private val prefs by lazy {
        context.getSharedPreferences("blue_player_covers", Context.MODE_PRIVATE)
    }

    suspend fun getCoverUrl(trackId: String, title: String, artist: String): String? =
        withContext(Dispatchers.IO) {
            prefs.getString(trackId, null)?.let { return@withContext it }
            val url = fetchFromItunes(title, artist)
            if (url != null) {
                prefs.edit().putString(trackId, url).apply()
            }
            url
        }

    private fun fetchFromItunes(title: String, artist: String): String? {
        return try {
            val term = URLEncoder.encode("$title $artist", "UTF-8")
            val requestUrl =
                "https://itunes.apple.com/search?term=$term&media=music&entity=song&limit=1"

            val connection = URL(requestUrl).openConnection() as HttpURLConnection
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.requestMethod = "GET"

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()

            val json = JSONObject(body)
            val results = json.optJSONArray("results") ?: return null
            val first = results.optJSONObject(0) ?: return null
            val art = first.optString("artworkUrl100", "")
            if (art.isEmpty()) null else art.replace("100x100bb", "600x600bb").replace("100x100", "600x600")
        } catch (e: Exception) {
            null
        }
    }
}