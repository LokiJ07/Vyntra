package io.github.lokij07.vyntra.data

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import dev.ffmpegkit_maintained.ytdlp.YtDlp
import dev.ffmpegkit_maintained.ytdlp.YtDlpRequest
import io.github.lokij07.vyntra.model.SavedAudio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Downloads one audio-only stream from supported sources into app-private temp storage,
 * then publishes to Music/Vyntra through MediaStore (so other music players can see it).
 * Does NOT bypass DRM, grant download rights, or guarantee site compatibility.
 */
class LocalAudioDownloader(private val context: Context) {
    private val prefs = context.getSharedPreferences("vyntra_audio_history", Context.MODE_PRIVATE)

    suspend fun save(url: String, title: String, onProgress: (Int) -> Unit): SavedAudio = withContext(Dispatchers.IO) {
        require(AudioSourceRules.validHttpsUrl(url)) { "Enter a valid HTTPS link to supported content." }
        val workDir = File(context.cacheDir, "vyntra_audio_${System.nanoTime()}")
        check(workDir.mkdirs()) { "Could not create a temporary audio folder." }
        try {
            YtDlp.init(context.applicationContext)
            val request = YtDlpRequest(url)
                .setOutputTemplate(File(workDir, "audio.%(ext)s").absolutePath)
                .addOption("--no-playlist")
                .addOption("--no-mtime")
                .addOption("--retries", "2")
                .addOption("-f", "bestaudio[ext=m4a]/bestaudio")
            val response = YtDlp.execute(request) { percentage, _, _ ->
                onProgress(percentage.toInt().coerceIn(0, 99))
            }
            check(response.isSuccess) {
                "The source did not provide an accessible audio download (exit code ${response.exitCode}). " +
                    "It may be restricted, unavailable or require permission."
            }
            val audio = workDir.listFiles().orEmpty().filter { file ->
                file.isFile && file.length() > 0L &&
                    AudioSourceRules.supportedAudioExtension(file.extension)
            }.maxByOrNull { it.length() }
                ?: error("No valid audio file was produced. Some sources block extraction or offer unsupported formats.")
            val ext = audio.extension.lowercase()
            val cleanTitle = AudioSourceRules.safeDisplayName(title)
            val outputName = "$cleanTitle-${System.currentTimeMillis()}.$ext"
            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, outputName)
                put(MediaStore.Audio.Media.MIME_TYPE, AudioSourceRules.mimeType(ext))
                put(MediaStore.Audio.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MUSIC}/Vyntra/")
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
                ?: error("Could not save audio to your Music folder.")
            try {
                resolver.openOutputStream(uri)?.use { output -> audio.inputStream().use { it.copyTo(output) } }
                    ?: error("Unable to write the audio file.")
                resolver.update(uri, ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) }, null, null)
            } catch (ex: Exception) {
                resolver.delete(uri, null, null)
                throw ex
            }
            val saved = SavedAudio(uri.toString(), cleanTitle, outputName, AudioSourceRules.mimeType(ext), System.currentTimeMillis())
            saveRecord(saved)
            onProgress(100)
            saved
        } finally {
            workDir.deleteRecursively()
        }
    }

    fun list(): List<SavedAudio> = try {
        val json = JSONArray(prefs.getString("saved_audio", "[]"))
        buildList {
            for (i in 0 until json.length()) {
                val obj = json.getJSONObject(i)
                add(SavedAudio(obj.getString("uri"), obj.getString("title"),
                    obj.getString("fileName"), obj.getString("mime"), obj.getLong("dateMillis")))
            }
        }
    } catch (_: Exception) { emptyList() }

    private fun saveRecord(saved: SavedAudio) {
        val array = JSONArray()
        (listOf(saved) + list()).take(100).forEach { audio ->
            array.put(JSONObject().apply {
                put("uri", audio.uri); put("title", audio.title); put("fileName", audio.fileName)
                put("mime", audio.mime); put("dateMillis", audio.dateMillis)
            })
        }
        prefs.edit().putString("saved_audio", array.toString()).apply()
    }

    fun open(saved: SavedAudio) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse(saved.uri), saved.mime)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Play with LokiTunes or another player").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}
