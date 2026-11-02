package io.github.lokij07.vyntra.data

import java.net.URI

/** Conservative input validation; the third-party extractor decides if the site supports audio. */
object AudioSourceRules {
    fun validHttpsUrl(raw: String): Boolean = try {
        val uri = URI(raw.trim())
        uri.scheme.equals("https", ignoreCase = true) && !uri.host.isNullOrBlank() &&
            uri.userInfo == null && uri.host!!.contains('.') && raw.length <= 2048
    } catch (_: Exception) { false }

    fun safeDisplayName(raw: String): String {
        val clean = raw.replace(Regex("[\\/:*?\"<>|\r\n\t]"), " ")
            .replace(Regex("""\s+"""), " ").trim().trim('.')
        return clean.take(100).ifBlank { "Vyntra Audio" }
    }

    fun supportedAudioExtension(ext: String): Boolean = ext.lowercase() in
        setOf("mp3", "m4a", "aac", "ogg", "opus", "flac", "wav", "webm")

    fun mimeType(ext: String): String = when (ext.lowercase()) {
        "mp3" -> "audio/mpeg"
        "m4a" -> "audio/mp4"
        "aac" -> "audio/aac"
        "ogg", "opus" -> "audio/ogg"
        "flac" -> "audio/flac"
        "wav" -> "audio/wav"
        "webm" -> "audio/webm"
        else -> "application/octet-stream"
    }
}
