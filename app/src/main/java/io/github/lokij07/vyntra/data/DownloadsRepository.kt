package io.github.lokij07.vyntra.data

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import io.github.lokij07.vyntra.model.DownloadRecord
import org.json.JSONArray
import org.json.JSONObject

/** The Android OS owns the actual transfers; Vyntra stores only lightweight history. */
class DownloadsRepository(private val context: Context) {
    private val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val prefs = context.getSharedPreferences("vyntra_history_v1", Context.MODE_PRIVATE)

    fun enqueue(url: String, title: String, creator: String, license: String,
                licenseUrl: String = "", sourcePage: String = "", ext: String): DownloadRecord {
        require(url.startsWith("https://", ignoreCase = true)) { "Only HTTPS download links are supported." }
        val fileName = "${System.currentTimeMillis()}_${AudioFiles.safeName(title)}.$ext"
        val request = DownloadManager.Request(Uri.parse(url)).apply {
            setTitle(title)
            addRequestHeader("User-Agent", "Vyntra/0.1.5 (Android)")
            setDescription("Vyntra • $creator")
            setMimeType(AudioFiles.mimeFor(ext))
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setAllowedOverMetered(true)
            setAllowedOverRoaming(false)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Vyntra/$fileName")
        }
        val id = manager.enqueue(request)
        val record = DownloadRecord(id, title, fileName, creator, license, licenseUrl, sourcePage, System.currentTimeMillis())
        save(record)
        return record
    }

    fun refresh(): List<DownloadRecord> = load().map { record ->
        try {
            manager.query(DownloadManager.Query().setFilterById(record.id)).use { cursor ->
                if (!cursor.moveToFirst()) {
                    record.copy(status = DownloadManager.STATUS_FAILED)
                } else {
                    val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    val soFar = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    val progress = if (total > 0) ((soFar * 100L) / total).toInt().coerceIn(0, 100) else null
                    val reason = if (status == DownloadManager.STATUS_FAILED || status == DownloadManager.STATUS_PAUSED)
                        cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON)) else null
                    val responseMime = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_MEDIA_TYPE))
                    record.copy(status = status, progress = progress, reason = reason, responseMime = responseMime)
                }
            }
        } catch (_: Exception) { record.copy(status = DownloadManager.STATUS_FAILED) }
    }

    fun cancel(record: DownloadRecord) {
        if (record.status != DownloadManager.STATUS_SUCCESSFUL ||
            DownloadHealth.isObviouslyNotAudio(record.responseMime)) manager.remove(record.id)
        val remaining = load().filterNot { it.id == record.id }
        persist(remaining)
    }

    fun open(record: DownloadRecord) {
        require(!DownloadHealth.isObviouslyNotAudio(record.responseMime)) {
            "The server saved a web page instead of audio. Please use a real audio download source."
        }
        val uri = manager.getUriForDownloadedFile(record.id)
            ?: error("File unavailable. It may have been moved or deleted.")
        val ext = record.filename.substringAfterLast('.', "mp3")
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, AudioFiles.mimeFor(ext))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Play with LokiTunes or another player").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    private fun save(record: DownloadRecord) = persist((listOf(record) + load()).take(200))

    private fun load(): List<DownloadRecord> {
        val raw = prefs.getString("history", "[]") ?: "[]"
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    add(DownloadRecord(
                        id = obj.getLong("id"),
                        title = obj.optString("title"),
                        filename = obj.optString("filename"),
                        creator = obj.optString("creator"),
                        licenseLabel = obj.optString("license"),
                        licenseUrl = obj.optString("licenseUrl"),
                        sourcePage = obj.optString("sourcePage"),
                        timeMillis = obj.optLong("time")
                    ))
                }
            }
        } catch (_: Exception) { emptyList() }
    }

    private fun persist(records: List<DownloadRecord>) {
        val array = JSONArray()
        records.forEach { item ->
            array.put(JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("filename", item.filename)
                put("creator", item.creator)
                put("license", item.licenseLabel)
                put("licenseUrl", item.licenseUrl)
                put("sourcePage", item.sourcePage)
                put("time", item.timeMillis)
            })
        }
        prefs.edit().putString("history", array.toString()).apply()
    }
}
