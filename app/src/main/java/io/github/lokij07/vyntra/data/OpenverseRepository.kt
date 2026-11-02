package io.github.lokij07.vyntra.data

import io.github.lokij07.vyntra.model.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL

/** Openverse indexes openly licensed audio. Always display the source and license. */
class OpenverseRepository {
    suspend fun search(query: String, page: Int): List<AudioTrack> = withContext(Dispatchers.IO) {
        val q = URLEncoder.encode(query.trim(), "UTF-8")
        val address = "https://api.openverse.org/v1/audio/?q=$q&page_size=20&page=$page"
        val connection = (URL(address).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 12000
            readTimeout = 12000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "Vyntra/0.1.0 (Android; personal music discovery)")
        }
        try {
            val status = connection.responseCode
            if (status == 429) error("Openverse rate limit reached. Try again shortly.")
            if (status !in 200..299) error("Search is unavailable (HTTP $status).")
            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val array = JSONObject(body).optJSONArray("results") ?: return@withContext emptyList()
            buildList {
                for (index in 0 until array.length()) {
                    val obj = array.optJSONObject(index) ?: continue
                    val directUrl = obj.optString("url").trim()
                    // DownloadManager/MediaPlayer use HTTPS; do not fetch arbitrary cleartext URLs.
                    if (!directUrl.startsWith("https://", ignoreCase = true)) continue
                    if (obj.optBoolean("mature", false)) continue
                    add(
                        AudioTrack(
                            id = obj.optString("id", "item_$index"),
                            title = obj.optString("title").ifBlank { "Untitled audio" },
                            creator = obj.optString("creator").ifBlank { "Unknown artist" },
                            audioUrl = directUrl,
                            landingUrl = obj.optString("foreign_landing_url"),
                            license = obj.optString("license"),
                            licenseVersion = obj.optString("license_version"),
                            licenseUrl = obj.optString("license_url"),
                            fileType = obj.optString("filetype"),
                            provider = obj.optString("source").ifBlank { "Openverse" },
                            attribution = obj.optString("attribution")
                        )
                    )
                }
            }
        } finally {
            connection.disconnect()
        }
    }
}
