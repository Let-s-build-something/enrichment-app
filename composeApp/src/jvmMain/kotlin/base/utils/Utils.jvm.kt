package base.utils

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import io.github.vinceglb.filekit.core.PlatformFile
import org.jetbrains.skia.Image
import java.security.MessageDigest

/** Returns a bitmap from a given file */
actual suspend fun getBitmapFromFile(file: PlatformFile): ImageBitmap? {
    return Image.makeFromEncoded(file.readBytes()).toComposeImageBitmap()
}

/**
 * Converts any value to SHA-256 hash
 * @return the generated SHA-256 hash
 */
actual fun sha256(value: Any?): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(value.toString().toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}
