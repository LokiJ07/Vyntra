package io.github.lokij07.vyntra.model

import android.app.DownloadManager

data class DownloadRecord(
    val id: Long,
    val title: String,
    val filename: String,
    val creator: String,
    val licenseLabel: String,
    val licenseUrl: String,
    val sourcePage: String,
    val timeMillis: Long,
    val status: Int = DownloadManager.STATUS_PENDING,
    val progress: Int? = null
)
