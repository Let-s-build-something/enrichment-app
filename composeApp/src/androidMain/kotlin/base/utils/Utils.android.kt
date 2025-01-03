package base.utils

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import io.github.vinceglb.filekit.core.PlatformFile
import java.security.MessageDigest

/** Returns a bitmap from a given file */
actual suspend fun getBitmapFromFile(file: PlatformFile): ImageBitmap? {
    return BitmapFactory.decodeByteArray(
        file.readBytes(),
        0,
        file.getSize()?.toInt() ?: 0
    )?.asImageBitmap()
}

/**
 * Converts any value to SHA-256 hash
 * @return the generated SHA-256 hash
 */
actual fun sha256(value: Any?): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(value.toString().toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}