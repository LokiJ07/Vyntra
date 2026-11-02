package io.github.lokij07.vyntra.data

import java.net.URI

object AudioFiles {
    private val extensions = setOf("mp3", "m4a", "flac", "ogg", "opus", "wav", "aac")

    fun safeName(title: String): String = title
        .replace(Regex("[^\\p{L}\\p{N} ._-]"), "_")
        .trim(' ', '.', '_')
        .take(70)
        .ifBlank { "Audio" }

    fun extensionFor(type: String, url: String): String? {
        val declared = type.lowercase().removePrefix("audio/").trim()
        when (declared) {
            "mpeg", "mp3" -> return "mp3"
            "mp4", "x-m4a", "m4a" -> return "m4a"
            "x-flac", "flac" -> return "flac"
            "ogg", "vorbis" -> return "ogg"
            "opus" -> return "opus"
            "x-wav", "wav", "wave" -> return "wav"
            "aac", "x-aac" -> return "aac"
        }
        val path = try { URI(url).path.orEmpty() } catch (_: Exception) { "" }
        val ext = path.substringAfterLast('.', "").lowercase()
        return ext.takeIf { it in extensions }
    }

    fun directLinkExtension(url: String): String? {
        if (!url.startsWith("https://", ignoreCase = true)) return null
        val uri = try { URI(url) } catch (_: Exception) { return null }
        if (uri.host.isNullOrBlank() || uri.userInfo != null) return null
        val ext = uri.path.orEmpty().substringAfterLast('.', "").lowercase()
        return ext.takeIf { it in extensions }
    }

    fun mimeFor(extension: String): String = when (extension) {
        "mp3" -> "audio/mpeg"
        "m4a" -> "audio/mp4"
        "flac" -> "audio/flac"
        "ogg" -> "audio/ogg"
        "opus" -> "audio/opus"
        "wav" -> "audio/wav"
        "aac" -> "audio/aac"
        else -> "audio/*"
    }
}
