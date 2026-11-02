package io.github.lokij07.vyntra.data

import io.github.lokij07.vyntra.model.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Official artist/song discovery. Apple previews are streaming-only promotional material. */
class ItunesRepository {
    suspend fun search(query: String, page: Int): SearchResult = withContext(Dispatchers.IO) {
        if (page > 1) return@withContext SearchResult(emptyList(), false)
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val url = "https://itunes.apple.com/search?term=$encoded&country=PH&media=music&entity=song&limit=50"
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 12000
            readTimeout = 15000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "Vyntra/0.1.3 (music discovery)")
        }
        try {
            val status = connection.responseCode
            if (status == 429) error("Official catalog is rate-limiting requests. Try later.")
            if (status !in 200..299) error("Official catalog search failed (HTTP $status).")
            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val items = JSONObject(body).optJSONArray("results") ?: return@withContext SearchResult(emptyList(), false)
            val songs = buildList {
                for (index in 0 until items.length()) {
                    val obj = items.optJSONObject(index) ?: continue
                    if (obj.optString("kind") != "song") continue
                    val id = obj.optString("trackId")
                    if (id.isBlank()) continue
                    val storeUrl = obj.optString("trackViewUrl")
                    if (!storeUrl.startsWith("https://")) continue
                    add(AudioTrack(
                        id = "itunes_$id",
                        title = obj.optString("trackName").ifBlank { "Untitled song" },
                        creator = obj.optString("artistName").ifBlank { "Unknown artist" },
                        audioUrl = obj.optString("previewUrl").takeIf { it.startsWith("https://") }.orEmpty(),
                        landingUrl = storeUrl,
                        license = "Official song catalog",
                        licenseVersion = "",
                        licenseUrl = storeUrl,
                        fileType = "m4a",
                        provider = "iTunes Store",
                        attribution = "${obj.optString("trackName")} — ${obj.optString("artistName")} | iTunes Store",
                        downloadable = false,
                        isStoreCatalog = true
                    ))
                }
            }
            SearchResult(songs, false)
        } finally { connection.disconnect() }
    }
}
