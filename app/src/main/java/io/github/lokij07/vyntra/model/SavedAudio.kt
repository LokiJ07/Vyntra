package io.github.lokij07.vyntra.model

data class SavedAudio(
    val uri: String,
    val title: String,
    val fileName: String,
    val mime: String,
    val dateMillis: Long
)
