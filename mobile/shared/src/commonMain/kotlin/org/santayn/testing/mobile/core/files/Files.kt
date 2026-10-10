package org.santayn.testing.mobile.core.files

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.cacheDir
import io.github.vinceglb.filekit.dialogs.openFileWithDefaultApplication
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import io.github.vinceglb.filekit.size
import io.github.vinceglb.filekit.write
import org.santayn.testing.mobile.core.network.DownloadedFile
import org.santayn.testing.mobile.core.network.UploadFile

/** Максимальный размер файла на загрузку (spring.servlet.multipart.max-file-size). */
const val MAX_UPLOAD_BYTES = 50L * 1024 * 1024

private val unsafeChars = Regex("""[\/:*?"<>|\u0000-\u001f]""")

fun safeFileName(name: String): String =
    name.replace(unsafeChars, "_").trim().ifBlank { "file" }

/** Сохраняет скачанный файл во временную папку приложения. */
suspend fun DownloadedFile.saveToCache(): PlatformFile {
    val target = FileKit.cacheDir / safeFileName(fileName)
    target.write(bytes)
    return target
}

/** Открывает файл во внешнем приложении (просмотрщик PDF, Word и т.п.). */
fun openWithDefaultApp(file: PlatformFile) {
    FileKit.openFileWithDefaultApplication(file)
}

/** Читает выбранный пользователем файл для multipart-загрузки. */
suspend fun PlatformFile.toUploadFile(): UploadFile {
    val size = runCatching { size() }.getOrDefault(0L)
    if (size > MAX_UPLOAD_BYTES) {
        throw IllegalArgumentException("Файл «$name» больше 50 МБ")
    }
    return UploadFile(name = name, bytes = readBytes(), contentType = guessContentType(name))
}

fun guessContentType(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
    "pdf" -> "application/pdf"
    "doc" -> "application/msword"
    "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    "ppt" -> "application/vnd.ms-powerpoint"
    "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
    "xls" -> "application/vnd.ms-excel"
    "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    "txt" -> "text/plain"
    "sql" -> "application/sql"
    "png" -> "image/png"
    "jpg", "jpeg" -> "image/jpeg"
    "zip" -> "application/zip"
    "mp4" -> "video/mp4"
    else -> "application/octet-stream"
}

/** Человекочитаемый размер файла. */
fun formatFileSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes Б"
    bytes < 1024 * 1024 -> "${(bytes / 102.4).toLong() / 10.0} КБ"
    else -> "${(bytes / (1024 * 102.4)).toLong() / 10.0} МБ"
}
