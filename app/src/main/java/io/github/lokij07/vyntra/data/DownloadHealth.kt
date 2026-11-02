package io.github.lokij07.vyntra.data

/** Human-friendly diagnostics for Android DownloadManager (reason codes are documented by Android). */
object DownloadHealth {
    fun failure(reason: Int?): String = when (reason) {
        401 -> "HTTP 401: the server requires authorization; Vyntra cannot access this file."
        403 -> "HTTP 403: the source denied this download. Check the track's permissions."
        404 -> "HTTP 404: the source audio file could not be found."
        408, 504 -> "The audio server timed out. Try again when the network is stable."
        429 -> "HTTP 429: source is rate-limiting requests. Try again later."
        in 500..599 -> "HTTP $reason: the audio server is temporarily unavailable."
        1001 -> "Unable to save file (storage error). Check free space and Downloads access."
        1002 -> "The server returned an unsupported HTTP response."
        1004 -> "Network transfer failed. Check connectivity and try again."
        1005 -> "Too many redirects. The audio link may be invalid."
        1006 -> "Not enough storage space on your device."
        1007 -> "Download storage is not available."
        1008 -> "Interrupted transfer could not resume. Try again."
        1009 -> "A file with the same name already exists."
        1000 -> "The Android download service reported an unknown error."
        null -> "No failure reason from Android."
        else -> if (reason in 400..599) "HTTP $reason: the server refused the request."
                else "Android download error code $reason."
    }

    fun pause(reason: Int?): String = when (reason) {
        1 -> "Waiting before retrying the network request."
        2 -> "Waiting for an internet connection."
        3 -> "Waiting for Wi-Fi."
        4 -> "Paused by Android."
        else -> "Waiting for the download to resume."
    }

    /** Avoid treating an HTML/API error page saved with .mp3 extension as a valid song. */
    fun isObviouslyNotAudio(mime: String?): Boolean {
        val value = mime?.substringBefore(';')?.trim()?.lowercase() ?: return false
        return value.startsWith("text/") || value in setOf(
            "application/json", "application/xml", "application/xhtml+xml",
            "application/problem+json"
        )
    }
}
