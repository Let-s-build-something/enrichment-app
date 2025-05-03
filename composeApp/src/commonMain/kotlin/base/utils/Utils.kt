package base.utils

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import app.cash.paging.compose.LazyPagingItems
import augmy.interactive.shared.ui.base.LocalDeviceType
import coil3.toUri
import dev.gitlive.firebase.storage.Data
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
fun getMediaType(mimeType: String): MediaType {
    return when {
        mimeType.lowercase().contains("gif") -> MediaType.GIF
        mimeType.lowercase().contains("image") -> MediaType.IMAGE
        mimeType.lowercase().contains("video") -> MediaType.VIDEO
        mimeType.lowercase().contains("audio") -> MediaType.AUDIO
        mimeType.lowercase().contains("text") -> MediaType.TEXT
        mimeType.lowercase().contains("pdf") -> MediaType.PDF
        mimeType.lowercase().contains("powerpoint")
                || mimeType.lowercase().contains("presentation")
                || mimeType.lowercase().contains("slideshow") -> MediaType.PRESENTATION
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

    val isVisual: Boolean
        get() = this == IMAGE || this == VIDEO || this == GIF
}

val maxMultiLineHeight: Int
    @Composable
    get() = when(LocalDeviceType.current) {
        WindowWidthSizeClass.Compact -> 5
        WindowWidthSizeClass.Medium -> 8
        else -> 15
    }

/** Returns a bitmap from a given file */
expect suspend fun getBitmapFromFile(file: PlatformFile): ImageBitmap?

/**
 * Converts any value to SHA-256 hash
 * @return the generated SHA-256 hash
 */
expect fun sha256(value: Any?): String

/** Retrieves the current device name */
expect fun deviceName(): String?

/** Converts ByteArray, which is more Java specific into Data, which is multiplatform */
expect fun fromByteArrayToData(byteArray: ByteArray): Data
