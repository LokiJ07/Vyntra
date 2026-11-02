package io.github.lokij07.vyntra.data

import io.github.lokij07.vyntra.model.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Artist-published songs. Downloads are offered only when the creator enables them. */
class AudiusRepository {
    suspend fun search(query: String, page: Int): SearchResult = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val url = "https://api.audius.co/v1/tracks/search?query=$encoded&limit=20&offset=${(page-1)*20}&app_name=Vyntra"
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 12000
            readTimeout = 15000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "Vyntra/0.1.3 (Android music discovery)")
        }
        try {
            val status = connection.responseCode
            if (status == 429) error("Audius is limiting requests. Try again later.")
            if (status !in 200..299) error("Audius search failed (HTTP $status). Try again.")
            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val results = JSONObject(body).optJSONArray("data") ?: return@withContext SearchResult(emptyList(), false)
            val songs = buildList {
                for (index in 0 until results.length()) {
                    val obj = results.optJSONObject(index) ?: continue
                    val id = obj.optString("id").trim()
                    if (id.isEmpty()) continue
                    val user = obj.optJSONObject("user")
                    val artist = user?.optString("name").orEmpty().ifBlank { user?.optString("handle") ?: "Unknown artist" }
                    val rawPath = obj.optString("permalink").trim()
                    val source = when {
                        rawPath.startsWith("https://") -> rawPath
                        rawPath.startsWith("/") -> "https://audius.co$rawPath"
                        rawPath.isNotBlank() -> "https://audius.co/${rawPath.trimStart('/')}"
                        else -> "https://audius.co/"
                    }
                    val enabled = obj.optBoolean("downloadable", obj.optBoolean("is_downloadable", false))
                    val hasGate = !obj.isNull("download_conditions") && obj.has("download_conditions")
                    val permitted = enabled && !hasGate
                    val stream = "https://api.audius.co/v1/tracks/$id/stream?app_name=Vyntra"
                    val download = if (permitted) "https://api.audius.co/v1/tracks/$id/download?app_name=Vyntra&format=mp3" else null
                    add(AudioTrack(
                        id = "audius_$id",
                        title = obj.optString("title").ifBlank { "Untitled song" },
                        creator = artist,
                        audioUrl = stream,
                        landingUrl = source,
                        license = if (permitted) "Creator-enabled download" else "Streaming only",
                        licenseVersion = "",
                        licenseUrl = source,
                        fileType = "mp3",
                        provider = "Audius",
                        attribution = "${obj.optString("title")} — $artist | Audius: $source",
                        downloadable = permitted,
                        downloadUrl = download
                    ))
                }
            }
            SearchResult(songs, results.length() >= 20)
        } finally { connection.disconnect() }
    }
}
