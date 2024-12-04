package base

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import app.cash.paging.compose.LazyPagingItems
import io.github.vinceglb.filekit.core.PlatformFile

/** Returns item at a specific index and handles indexOutOfBounds exception */
fun <T: Any> LazyPagingItems<T>.getOrNull(index: Int): T? {
    return if(index >= this.itemCount) null else this[index]
}

/** Color derived from a user tag */
fun tagToColor(tag: String?) = if(tag != null) Color(("ff$tag").toLong(16)) else null

/** Converts color to 6 hexadecimal numbers representing it without transparency */
fun Color.asSimpleString() = this.value.toString(16).substring(2, 8)

/** Returns a media type of a file */
fun getMediaType(extension: String): MediaType {
    return when (extension.lowercase()) {
        "jpg", "jpeg", "png", "bmp", "gif", "svg", "webp"  -> MediaType.IMAGE
        "mp4", "avi", "mkv", "mov", "webm" -> MediaType.VIDEO
        "mp3", "wav", "aac", "flac", "ogg" -> MediaType.AUDIO
        "txt", "csv", "log" -> MediaType.TEXT
        "pdf" -> MediaType.PDF
        "ppt", "pptx" -> MediaType.PRESENTATION
        else -> MediaType.UNKNOWN
    }
}

/** Type of a media file */
enum class MediaType {
    IMAGE,
    VIDEO,
    AUDIO,
    TEXT,
    PDF,
    PRESENTATION,
    UNKNOWN
}

/** Returns a bitmap from a given file */
expect suspend fun getBitmapFromFile(file: PlatformFile): ImageBitmap?
