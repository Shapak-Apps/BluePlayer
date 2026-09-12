package com.blueplayer.core.player

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class OnlineCoverFetcher(
    private val context: Context,
    private val coverCache: CoverCache
) {

    companion object {
        private const val TAG = "OnlineCoverFetcher"
    }

    private val prefs by lazy {
        context.getSharedPreferences("blue_player_covers", Context.MODE_PRIVATE)
    }

    suspend fun getCoverUrl(trackId: String, title: String, artist: String): String? =
        withContext(Dispatchers.IO) {
            val cached = prefs.getString(trackId, null)
            if (cached == "NOT_FOUND") {
                Log.d(TAG, "[$trackId] NOT_FOUND in cache, skipping all sources")
                return@withContext null
            }
            if (cached != null) {
                Log.d(TAG, "[$trackId] Cache hit: $cached")
                coverCache.put(trackId, cached)
                return@withContext cached
            }

            Log.d(TAG, "[$trackId] Parallel fetch: $title - $artist")

            val cleanTitle = cleanTitle(title)
            val hasCleanVersion = cleanTitle != title

            Log.d(TAG, "[$trackId] Clean title: $cleanTitle")

            val url = coroutineScope {
                val jobs = mutableListOf<Deferred<String?>>()

                jobs.add(async { fetchAndValidate(title, artist, ::fetchFromItunes) })
                jobs.add(async { fetchAndValidate(title, artist, ::fetchFromDeezer) })
                jobs.add(async { fetchAndValidate(title, artist, ::fetchFromMusicBrainz) })

                if (hasCleanVersion && cleanTitle.isNotBlank()) {
                    jobs.add(async { fetchAndValidate(cleanTitle, artist, ::fetchFromItunes) })
                    jobs.add(async { fetchAndValidate(cleanTitle, artist, ::fetchFromDeezer) })
                    jobs.add(async { fetchAndValidate(cleanTitle, artist, ::fetchFromMusicBrainz) })
                }

                var found: String? = null
                for (job in jobs) {
                    val result = job.await()
                    if (result != null) {
                        found = result
                        jobs.forEach { it.cancel() }
                        break
                    }
                }
                found
            }

            if (url != null) {
                Log.d(TAG, "[$trackId] Found (validated): $url")
                prefs.edit().putString(trackId, url).apply()
                coverCache.put(trackId, url)
            } else {
                Log.d(TAG, "[$trackId] Not found in any source")
                prefs.edit().putString(trackId, "NOT_FOUND").apply()
            }
            url
        }

    private fun cleanTitle(title: String): String {
        var s = title
        s = s.replace(Regex("\\([^)]*\\b(?:www\\.|http|\\.ru|\\.com|\\.net|\\.me|\\.org|\\.info)\\b[^)]*\\)", RegexOption.IGNORE_CASE), "")
        s = s.replace(Regex("\\[[^\\]]*\\b(?:www\\.|http|\\.ru|\\.com|\\.net|\\.me|\\.org|\\.info)\\b[^\\]]*\\]", RegexOption.IGNORE_CASE), "")
        s = s.replace(Regex("\\b(?:via\\s+)?(?:offblogmedia|lightaudio|mp3tornado|zvyki|pelican-music|drivemusic|uzbmp3|mp3\\.pm)\\b[\\w.]*", RegexOption.IGNORE_CASE), "")
        s = s.replace(Regex("^(?:\\d+[_-])+"), "")
        val versionPatterns = listOf(
            "original\\s+mix", "extended\\s+mix", "radio\\s+edit",
            "epic\\s+version", "official\\s+music\\s+video", "music\\s+video",
            "mood\\s+video", "lyrics?\\s+video", "lyrics?", "visualizer",
            "sped\\s+up", "nightcore", "daycore", "bass\\s+boosted",
            "8d\\s+audio", "8d", "mashup", "flip", "bootleg", "edit",
            "vip\\s+mix", "club\\s+mix", "radio\\s+version"
        )
        versionPatterns.forEach { p ->
            s = s.replace(Regex("\\([^)]*\\b$p\\b[^)]*\\)", RegexOption.IGNORE_CASE), "")
            s = s.replace(Regex("\\[[^\\]]*\\b$p\\b[^\\]]*\\]", RegexOption.IGNORE_CASE), "")
        }
        s = s.replace(Regex("\\([^)]*\\b(?:ultra\\s+)?slowed(?:\\s*[+_]\\s*reverb)?\\b[^)]*\\)", RegexOption.IGNORE_CASE), "")
        s = s.replace(Regex("\\[[^\\]]*\\b(?:ultra\\s+)?slowed(?:\\s*[+_]\\s*reverb)?\\b[^\\]]*\\]", RegexOption.IGNORE_CASE), "")
        s = s.replace(Regex("\\b(?:ultra[_\\s-]*)?slowed(?:[_\\s+]*reverb)?\\b", RegexOption.IGNORE_CASE), "")
        s = s.replace(Regex("\\breverb\\b", RegexOption.IGNORE_CASE), "")
        s = s.replace(Regex("\\([^)]*\\b(?:[\\w-]+\\s+)?remix(?:ed|es)?\\b[^)]*\\)", RegexOption.IGNORE_CASE), "")
        s = s.replace(Regex("\\[[^\\]]*\\b(?:[\\w-]+\\s+)?remix(?:ed|es)?\\b[^\\]]*\\]", RegexOption.IGNORE_CASE), "")
        s = s.replace(Regex("\\b(?:[\\w-]+\\s+)?remix(?:ed|es)?\\b", RegexOption.IGNORE_CASE), "")
        s = s.replace(Regex("\\([^)]*\\b(?:live|acoustic|cover|instrumental|karaoke|demo|unreleased|official\\s+audio)\\b[^)]*\\)", RegexOption.IGNORE_CASE), "")
        s = s.replace(Regex("\\b(?:live|acoustic|cover|instrumental|karaoke|demo)\\b", RegexOption.IGNORE_CASE), "")
        s = s.replace(Regex("\\bpt\\.?\\s*[ivx\\d]+\\b", RegexOption.IGNORE_CASE), "")
        s = s.replace(Regex("\\bpart\\s*[ivx\\d]+\\b", RegexOption.IGNORE_CASE), "")
        s = s.replace(Regex("\\bchapter\\s*[ivx\\d]+\\b", RegexOption.IGNORE_CASE), "")
        s = s.replace(Regex("\\b(?:clean|dirty|explicit|uncensored)\\s+(?:version|edit)?\\b", RegexOption.IGNORE_CASE), "")
        s = s.replace(Regex("\\b(?:clean|dirty|explicit|uncensored)\\b", RegexOption.IGNORE_CASE), "")
        s = s.replace(Regex("(\\(\\s*(?:feat|ft|featuring)\\.?\\s+[\\w.]+)(?:[,;]\\s*[\\w.\\s]+)+\\s*\\)", RegexOption.IGNORE_CASE), "$1)")
        s = s.replace(Regex("(\\b(?:feat|ft|featuring)\\.?\\s+[\\w.]+)(?:[,;]\\s*[\\w.\\s&]+)+", RegexOption.IGNORE_CASE), "$1")

        val parts = s.split(" - ", " – ", " — ", limit = 2)
        if (parts.size == 2) {
            val artists = parts[0].split(",", "，").map { it.trim() }.filter { it.isNotBlank() }
            if (artists.size > 2) {
                s = artists.take(2).joinToString(", ") + " - " + parts[1]
            }
        }

        s = s.replace(Regex("\\b\\d{3,4}\\s*(?:k|kbps|kb\\/s)\\b", RegexOption.IGNORE_CASE), "")
        s = s.replace(Regex("\\bHQ\\b", RegexOption.IGNORE_CASE), "")
        s = s.replace(Regex("\\bHD\\b", RegexOption.IGNORE_CASE), "")
        s = s.replace(Regex("\\bRapMusicHD\\b", RegexOption.IGNORE_CASE), "")
        s = s.replace(Regex("\\bSouthCentralChannel\\b", RegexOption.IGNORE_CASE), "")
        s = s.replace(Regex("\\bRoss\\s+Ross\\s+UK\\b", RegexOption.IGNORE_CASE), "")

        s = s.replace(Regex("\\(\\s*20[12]\\d\\s*\\)"), "")
        s = s.replace(Regex("\\[\\s*20[12]\\d\\s*\\]"), "")

        s = s.replace(Regex("[\\p{So}\\p{Sk}\\p{Cn}]"), "")

        s = s.replace('_', ' ')
        s = s.replace(Regex("\\s+"), " ")

        s = s.replace(Regex("\\(\\s*\\)"), "")
        s = s.replace(Regex("\\[\\s*\\]"), "")
        s = s.replace(Regex("\\{\\s*\\}"), "")

        s = s.replace(Regex("^\\s*[-–—:,;]+\\s*"), "")
        s = s.replace(Regex("\\s*[-–—:,;]+\\s*$"), "")
        s = s.replace(Regex("\\s*[+]+\\s*"), " ")

        s = s.replace(Regex("\\s*[-–—]\\s*$"), "")

        return s.trim()
    }

    fun clearCache() {
        prefs.edit().clear().apply()
        coverCache.clear()
    }

    fun clearCacheForTrack(trackId: String) {
        prefs.edit().remove(trackId).apply()
        coverCache.remove(trackId)
    }

    private suspend fun fetchAndValidate(
        title: String,
        artist: String,
        fetcher: (String, String) -> String?
    ): String? {
        val url = fetcher(title, artist) ?: return null
        return if (validateUrl(url)) url else null
    }

    private fun validateUrl(url: String): Boolean {
        return try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 3000
            connection.readTimeout = 3000
            connection.requestMethod = "HEAD"
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("User-Agent", "BluePlayer/1.0")

            val code = connection.responseCode
            val contentType = connection.contentType?.lowercase() ?: ""
            connection.disconnect()

            val ok = code == HttpURLConnection.HTTP_OK &&
                    (contentType.startsWith("image/") || url.contains("mzstatic") || url.contains("deezer"))

            if (!ok) {
                Log.d(TAG, "URL validation failed: HTTP $code, content-type=$contentType for $url")
            }
            ok
        } catch (e: Exception) {
            Log.d(TAG, "URL validation error: ${e.message} for $url")
            false
        }
    }

    private fun fetchFromItunes(title: String, artist: String): String? {
        return try {
            val term = URLEncoder.encode("$title $artist", "UTF-8")
            val requestUrl =
                "https://itunes.apple.com/search?term=$term&media=music&entity=song&limit=1"

            val connection = URL(requestUrl).openConnection() as HttpURLConnection
            connection.connectTimeout = 6000
            connection.readTimeout = 6000
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "BluePlayer/1.0")

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                connection.disconnect()
                return null
            }

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()

            val json = JSONObject(body)
            val resultCount = json.optInt("resultCount", 0)
            if (resultCount == 0) return null

            val results = json.optJSONArray("results") ?: return null
            val first = results.optJSONObject(0) ?: return null
            val art = first.optString("artworkUrl100", "")
            if (art.isEmpty()) null
            else art.replace("100x100bb", "600x600bb")
        } catch (e: Exception) {
            Log.e(TAG, "iTunes fetch failed: ${e.message}")
            null
        }
    }

    private fun fetchFromDeezer(title: String, artist: String): String? {
        return try {
            val term = URLEncoder.encode("$artist $title", "UTF-8")
            val requestUrl = "https://api.deezer.com/search/track?q=$term&limit=1"

            val connection = URL(requestUrl).openConnection() as HttpURLConnection
            connection.connectTimeout = 6000
            connection.readTimeout = 6000
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "BluePlayer/1.0")

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                connection.disconnect()
                return null
            }

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()

            val json = JSONObject(body)
            val data = json.optJSONArray("data") ?: return null
            if (data.length() == 0) return null
            val first = data.optJSONObject(0) ?: return null
            val album = first.optJSONObject("album") ?: return null
            val cover = album.optString("cover_xl", "")
                .ifEmpty { album.optString("cover_big", "") }
                .ifEmpty { album.optString("cover_medium", "") }
            if (cover.isEmpty()) null else cover
        } catch (e: Exception) {
            Log.e(TAG, "Deezer fetch failed: ${e.message}")
            null
        }
    }

    private fun fetchFromMusicBrainz(title: String, artist: String): String? {
        return try {
            val query = URLEncoder.encode("release:\"$title\" AND artist:\"$artist\"", "UTF-8")
            val searchUrl = "https://musicbrainz.org/ws/2/release?query=$query&fmt=json&limit=1"

            val connection = URL(searchUrl).openConnection() as HttpURLConnection
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "BluePlayer/1.0 (blueplayer.app)")

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                connection.disconnect()
                return null
            }

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()

            val json = JSONObject(body)
            val releases = json.optJSONArray("releases") ?: return null
            if (releases.length() == 0) return null
            val first = releases.optJSONObject(0) ?: return null
            val releaseId = first.optString("id", "")
            if (releaseId.isEmpty()) return null

            val coverUrl = "https://coverartarchive.org/release/$releaseId/front"
            val coverConnection = URL(coverUrl).openConnection() as HttpURLConnection
            coverConnection.connectTimeout = 5000
            coverConnection.readTimeout = 5000
            coverConnection.instanceFollowRedirects = true

            val ok = coverConnection.responseCode == HttpURLConnection.HTTP_OK
            coverConnection.disconnect()
            if (ok) coverUrl else null
        } catch (e: Exception) {
            Log.e(TAG, "MusicBrainz fetch failed: ${e.message}")
            null
        }
    }
}