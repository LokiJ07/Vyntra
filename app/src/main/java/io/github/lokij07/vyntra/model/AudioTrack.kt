package io.github.lokij07.vyntra.model

data class AudioTrack(
    val id: String,
    val title: String,
    val creator: String,
    val audioUrl: String,
    val landingUrl: String,
    val license: String,
    val licenseVersion: String,
    val licenseUrl: String,
    val fileType: String,
    val provider: String,
    val attribution: String,
    val downloadable: Boolean = true,
    val downloadUrl: String? = null,
    val isStoreCatalog: Boolean = false,
    val artworkUrl: String = ""
) {
    val licenseLabel: String
        get() = listOf(license.uppercase(), licenseVersion)
            .filter { it.isNotBlank() }.joinToString(" ")
}
