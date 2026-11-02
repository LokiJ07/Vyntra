package io.github.lokij07.vyntra.data

import io.github.lokij07.vyntra.model.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Jamendo's own API says download links are usable only when the artist allows them.
 * Register a Jamendo application and enter its public Client ID in Vyntra -> About.
 */
class JamendoRepository {
    suspend fun search(query: String, field: SearchField, clientId: String): SearchResult =
        withContext(Dispatchers.IO) {
            if (!JamendoPolicy.isClientIdValid(clientId)) {
                return@withContext SearchResult(emptyList(), false)
            }
            val encoded = URLEncoder.encode(query.trim(), "UTF-8")
            val url = "https://api.jamendo.com/v3.0/tracks/?client_id=$clientId" +
                "&format=json&limit=60&order=relevance&type=single+albumtrack" +
                "&search=$encoded&audiodlformat=mp32&include=licenses"
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 12000
                readTimeout = 15000
                setRequestProperty("User-Agent", "Vyntra/0.2.0 (Android music downloader)")
                setRequestProperty("Accept", "application/json")
            }
            try {
                val status = conn.responseCode
                if (status !in 200..299) error("Jamendo HTTP $status. Check the Client ID or try later.")
                val data = JSONObject(conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() })
                val headers = data.optJSONObject("headers")
                if (headers != null && headers.optString("status") != "success") {
                    error("Jamendo: ${headers.optString("error_message", "Invalid Client ID or API error")}")
                }
                val array = data.optJSONArray("results") ?: return@withContext SearchResult(emptyList(), false)
                val candidates = buildList {
                    for (i in 0 until array.length()) {
                        val item = array.optJSONObject(i) ?: continue
                        val id = item.optString("id")
                        val title = item.optString("name")
                        val artist = item.optString("artist_name")
                        val stream = item.optString("audio")
                        val direct = item.optString("audiodownload")
                        if (id.isBlank() || title.isBlank() || artist.isBlank()) continue
                        if (!JamendoPolicy.mayDownload(item.optBoolean("audiodownload_allowed", false), direct)) continue
                        val page = "https://www.jamendo.com/track/$id"
                        val license = item.optString("license_ccurl").replace("http://", "https://")
                        add(AudioTrack(
                            id = "jamendo_$id",
                            title = title,
                            creator = artist,
                            audioUrl = stream.takeIf { it.startsWith("https://") }.orEmpty(),
                            landingUrl = page,
                            license = "Creative Commons",
                            licenseVersion = "",
                            licenseUrl = license.ifBlank { page },
                            fileType = "mp3",
                            provider = "Jamendo",
                            attribution = "$title — $artist | Jamendo $page | $license",
                            downloadable = true,
                            downloadUrl = direct,
                            artworkUrl = item.optString("image")
                        ))
                    }
                }
                val results = candidates.mapNotNull { track ->
                    SearchRelevance.score(query, track.title, track.creator, field)?.let { it to track }
                }.sortedByDescending { it.first }.map { it.second }
                SearchResult(results, false)
            } finally { conn.disconnect() }
        }
}

