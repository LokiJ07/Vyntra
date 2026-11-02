package io.github.lokij07.vyntra.data

/** Plain Kotlin policy to test independently from Android and HTTP. */
object JamendoPolicy {
    fun isClientIdValid(value: String): Boolean = value.matches(Regex("[a-zA-Z0-9_-]{6,100}"))
    fun mayDownload(allowed: Boolean, url: String): Boolean =
        allowed && url.startsWith("https://", ignoreCase = true)
}
