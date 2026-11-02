package io.github.lokij07.vyntra.data

/** Turns extractor/server errors into short actionable status text. It never claims to bypass access restrictions. */
object AudioDownloadProblem {
    fun explain(raw: String?): String {
        val message = raw.orEmpty().lowercase()
        return when {
            Regex("(?:http(?: error)?[ :]+|status[=: ]+|code[=: ]+)(?:403|forbidden)\\b").containsMatchIn(message) ||
                "403 forbidden" in message || "http error 403" in message ->
                "HTTP 403: The source refused this audio request. This is a provider restriction, not a storage or APK error. Use an authorized direct audio download link or the source's official download option."
            "401" in message && ("http" in message || "unauthorized" in message) ->
                "HTTP 401: This source requires authorization. Vyntra cannot fetch private audio without source permission."
            "404" in message && ("http" in message || "not found" in message) ->
                "HTTP 404: The source audio is unavailable or the link has expired."
            "requested format is not available" in message || "format not available" in message ->
                "The source did not offer a supported audio-only format. Try another authorized track or direct audio link."
            "unsupported url" in message || "unsupported site" in message || "no suitable extractor" in message ->
                "This website is not supported by the bundled audio engine. Try a direct downloadable audio file link."
            "timed out" in message || "timeout" in message || "network is unreachable" in message ||
                "failed to establish" in message || "connection reset" in message ->
                "Network timeout or connection error. Check your connection and retry an accessible download."
            "no space left" in message || "enospc" in message ->
                "Not enough device storage. Free up space and retry."
            "no valid audio file" in message || "invalid audio header" in message ->
                "The source did not produce a valid playable audio file. Nothing was saved."
            message.isBlank() -> "Audio extraction did not complete. The source may be unavailable or restricted."
            else -> "Audio extraction failed. The source may be unavailable or restricted. Check the original download permissions."
        }
    }
}
