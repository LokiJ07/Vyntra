package io.github.lokij07.vyntra.data

import java.io.File

/** Check the container signature before saving; a .mp3 extension does not make an HTML error page audio. */
object AudioFileValidation {
    fun looksLikeAudio(file: File): Boolean {
        if (!file.isFile || file.length() < 12) return false
        val header = ByteArray(12)
        val read = try { file.inputStream().use { it.read(header) } } catch (_: Exception) { return false }
        if (read < 12) return false
        val ext = file.extension.lowercase()
        fun ascii(start: Int, text: String): Boolean = text.indices.all { index ->
            header[start + index].toInt() and 0xff == text[index].code
        }
        fun byte(index: Int): Int = header[index].toInt() and 0xff
        return when (ext) {
            "mp3" -> ascii(0, "ID3") || (byte(0) == 0xff && (byte(1) and 0xe0) == 0xe0)
            "m4a" -> ascii(4, "ftyp")
            "aac" -> (byte(0) == 0xff && (byte(1) and 0xf0) == 0xf0) || ascii(0, "ID3")
            "ogg", "opus" -> ascii(0, "OggS")
            "flac" -> ascii(0, "fLaC")
            "wav" -> ascii(0, "RIFF") && ascii(8, "WAVE")
            "webm" -> byte(0) == 0x1a && byte(1) == 0x45 && byte(2) == 0xdf && byte(3) == 0xa3
            else -> false
        }
    }
}
