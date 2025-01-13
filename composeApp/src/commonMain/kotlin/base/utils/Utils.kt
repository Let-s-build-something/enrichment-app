package base.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import app.cash.paging.compose.LazyPagingItems
import coil3.toUri
import io.github.vinceglb.filekit.core.PlatformFile

/** Returns item at a specific index and handles indexOutOfBounds exception */
fun <T: Any> LazyPagingItems<T>.getOrNull(index: Int): T? {
    return if(index >= this.itemCount || index < 0) null else this[index]
}

/** Color derived from a user tag */
fun tagToColor(tag: String?) = if(tag != null) Color(("ff$tag").toLong(16)) else null

/** Converts color to 6 hexadecimal numbers representing it without transparency */
fun Color.asSimpleString() = this.value.toString(16).substring(2, 8)

/** Returns a media type of a file */
fun getMediaType(url: String): MediaType {
    return when (getUrlExtension(url).lowercase()) {
        "jpg", "jpeg", "png", "bmp", "svg", "webp", "avif" -> MediaType.IMAGE
        "gif" -> MediaType.GIF
        "mp4", "avi", "mkv", "mov", "webm" -> MediaType.VIDEO
        "mp3", "wav", "aac", "flac", "ogg" -> MediaType.AUDIO
        "txt", "csv", "log" -> MediaType.TEXT
        "pdf" -> MediaType.PDF
        "ppt", "pptx" -> MediaType.PRESENTATION
        else -> MediaType.UNKNOWN
    }
}

/** Returns the extension of an url */
fun getUrlExtension(url: String): String = (url.toUri().path ?: url).substringAfterLast(".").lowercase()

/** Type of a media file */
enum class MediaType {
    IMAGE,
    VIDEO,
    GIF,
    AUDIO,
    TEXT,
    PDF,
    PRESENTATION,
    UNKNOWN;

    /** Whether this media can be or is visualized */
    val isVisualized: Boolean
        get() = this == IMAGE || this == VIDEO || this == GIF
}

/** Returns a bitmap from a given file */
expect suspend fun getBitmapFromFile(file: PlatformFile): ImageBitmap?

/**
 * Converts any value to SHA-256 hash
 * @return the generated SHA-256 hash
 */
expect fun sha256(value: Any?): String
