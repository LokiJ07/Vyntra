package io.github.lokij07.vyntra.data

import io.github.lokij07.vyntra.model.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URLEncoder
import java.net.URL

/** Openverse indexes openly licensed audio. Always display the source and license. */
class OpenverseRepository {
    suspend fun search(query: String, page: Int): SearchResult = withContext(Dispatchers.IO) {
        val q = URLEncoder.encode(query.trim(), "UTF-8")
        val address = "https://api.openverse.org/v1/audio/?q=$q&page_size=20&page=$page"
        val connection = (URL(address).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 6500
            readTimeout = 9500
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "Vyntra/0.1.2 (Android; personal music discovery)")
        }
        try {
            val status = try { connection.responseCode } catch (_: SocketTimeoutException) {
                error("Openverse search is timing out (known service issue). Choose Wikimedia Commons instead.")
            }
            if (status == 429) error("Openverse rate limit reached. Try again shortly.")
            if (status == 502 || status == 503 || status == 504) {
                error("Openverse is temporarily unavailable (HTTP $status). Try Wikimedia Commons.")
            }
            if (status !in 200..299) error("Openverse search unavailable (HTTP $status).")
            val body = try {
                connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } catch (_: SocketTimeoutException) {
                error("Openverse search is timing out (known service issue). Choose Wikimedia Commons instead.")
            }
            val json = JSONObject(body)
            val array = json.optJSONArray("results") ?: return@withContext SearchResult(emptyList(), false)
            val found = buildList {
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
            SearchResult(found, page < json.optInt("page_count", page))
        } finally {
            connection.disconnect()
        }
    }
}
