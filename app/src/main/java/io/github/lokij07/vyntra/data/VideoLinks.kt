package io.github.lokij07.vyntra.data

import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/** An actual video URL is not an audio download URL. Utilities never extract media. */
object VideoLinks {
    fun searchUrl(query: String): String =
        "https://m.youtube.com/results?search_query=" +
            URLEncoder.encode(query.trim(), StandardCharsets.UTF_8.toString())

    /** Canonicalize selected video pages; ignore search/home/channel pages and unsafe schemes. */
    fun selectedVideoUrl(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val uri = try { URI(raw.trim()) } catch (_: Exception) { return null }
        if (uri.scheme?.lowercase() != "https" && uri.scheme?.lowercase() != "http") return null
        val host = uri.host?.lowercase() ?: return null
        val id = when {
            host == "youtu.be" || host == "www.youtu.be" ->
                uri.path?.trim('/')?.substringBefore('/')
            host == "youtube.com" || host == "www.youtube.com" ||
                host == "m.youtube.com" || host == "music.youtube.com" -> {
                when {
                    uri.path == "/watch" -> uri.rawQuery?.split('&')
                        ?.firstOrNull { it.substringBefore('=') == "v" }
                        ?.substringAfter('=', "")
                    uri.path?.startsWith("/shorts/") == true ->
                        uri.path.removePrefix("/shorts/").substringBefore('/')
                    uri.path?.startsWith("/live/") == true ->
                        uri.path.removePrefix("/live/").substringBefore('/')
                    else -> null
                }
            }
            else -> null
        }
        return if (id != null && id.matches(Regex("[A-Za-z0-9_-]{11}")))
            "https://www.youtube.com/watch?v=$id" else null
    }
}
