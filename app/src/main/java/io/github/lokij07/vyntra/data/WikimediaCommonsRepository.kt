package io.github.lokij07.vyntra.data

import android.text.Html
import io.github.lokij07.vyntra.model.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URLEncoder
import java.net.URL

/**
 * Searches freely licensed audio files hosted on Wikimedia Commons.
 * This is independent of Openverse's currently unreliable search backend.
 */
class WikimediaCommonsRepository {
    suspend fun search(query: String, page: Int): SearchResult = withContext(Dispatchers.IO) {
        val params = linkedMapOf(
            "action" to "query",
            "generator" to "search",
            "gsrsearch" to "filetype:audio ${query.trim()}",
            "gsrnamespace" to "6", // File: namespace only
            "gsrlimit" to "20",
            "gsroffset" to ((page - 1).coerceAtLeast(0) * 20).toString(),
            "prop" to "imageinfo",
            "iiprop" to "url|mime|user|extmetadata",
            "iiextmetadatafilter" to "LicenseShortName|LicenseUrl|Artist|Credit|AttributionRequired|UsageTerms",
            "format" to "json",
            "formatversion" to "2"
        )
        val arguments = params.entries.joinToString("&") {
            "${URLEncoder.encode(it.key, "UTF-8")}=${URLEncoder.encode(it.value, "UTF-8")}" 
        }
        val address = "https://commons.wikimedia.org/w/api.php?$arguments"
        val connection = (URL(address).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8000
            readTimeout = 15000
            setRequestProperty("Accept", "application/json")
            // Identifiable User-Agent is required for Wikimedia APIs.
            setRequestProperty("User-Agent", "Vyntra/0.1.2 (https://github.com/LokiJ07/Vyntra; Android audio discovery)")
        }
        try {
            val status = try { connection.responseCode } catch (_: SocketTimeoutException) {
                error("Wikimedia Commons did not respond. Check Wi-Fi/mobile data and retry.")
            }
            when (status) {
                429 -> error("Wikimedia Commons is rate-limiting requests. Try again later.")
                403 -> error("Wikimedia Commons refused the search request (HTTP 403).")
            }
            if (status !in 200..299) error("Wikimedia Commons search failed (HTTP $status).")
            val body = try {
                connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } catch (_: SocketTimeoutException) {
                error("Wikimedia Commons search timed out. Try again later.")
            }
            val root = JSONObject(body)
            if (root.has("error")) {
                val apiError = root.optJSONObject("error")
                error(apiError?.optString("info")?.take(180)?.takeIf { it.isNotBlank() }
                    ?: "Wikimedia Commons returned an API error.")
            }
            val pages = root.optJSONObject("query")?.optJSONArray("pages")
                ?: return@withContext SearchResult(emptyList(), false)
            val tracks = buildList {
                for (i in 0 until pages.length()) {
                    val entry = pages.optJSONObject(i) ?: continue
                    val info = entry.optJSONArray("imageinfo")?.optJSONObject(0) ?: continue
                    val url = info.optString("url")
                    val mime = info.optString("mime")
                    if (!url.startsWith("https://", ignoreCase = true)) continue
                    if (!mime.startsWith("audio/", ignoreCase = true) &&
                        mime.lowercase() != "application/ogg") continue
                    if (AudioFiles.extensionFor(mime, url) == null) continue
                    val metadata = info.optJSONObject("extmetadata") ?: JSONObject()
                    fun meta(name: String) = metadata.optJSONObject(name)?.optString("value").orEmpty()
                    val licenseName = plain(meta("LicenseShortName"))
                        .ifBlank { plain(meta("UsageTerms")) }
                        .ifBlank { "See file page" }
                    val creator = plain(meta("Artist"))
                        .ifBlank { info.optString("user") }
                        .ifBlank { "Wikimedia Commons contributor" }
                    val title = entry.optString("title")
                        .removePrefix("File:").substringBeforeLast('.').replace('_', ' ')
                        .ifBlank { "Untitled audio" }
                    add(AudioTrack(
                        id = "commons_${entry.optLong("pageid", i.toLong())}",
                        title = title,
                        creator = creator,
                        audioUrl = url,
                        landingUrl = info.optString("descriptionurl")
                            .ifBlank { "https://commons.wikimedia.org/wiki/Special:Redirect/file/${URLEncoder.encode(entry.optString("title").removePrefix("File:"), "UTF-8")}" },
                        license = licenseName,
                        licenseVersion = "",
                        licenseUrl = plain(meta("LicenseUrl")),
                        fileType = mime,
                        provider = "Wikimedia Commons",
                        attribution = plain(meta("Credit")).ifBlank {
                            "$title — $creator | $licenseName | ${info.optString("descriptionurl")}" 
                        }
                    ))
                }
            }
            SearchResult(tracks, root.optJSONObject("continue")?.has("gsroffset") == true)
        } finally {
            connection.disconnect()
        }
    }

    private fun plain(html: String): String = Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT)
        .toString().replace(Regex("\\s+"), " ").trim().take(300)
}
