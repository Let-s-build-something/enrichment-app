package base.utils

import android.content.ClipData
import android.content.Context
import android.graphics.BitmapFactory
import android.provider.Settings
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readBytes
import io.github.vinceglb.filekit.size
import org.koin.mp.KoinPlatform.getKoin
import java.security.MessageDigest

/** Returns a bitmap from a given file */
actual suspend fun getBitmapFromFile(file: PlatformFile): ImageBitmap? {
    return BitmapFactory.decodeByteArray(
        file.readBytes(),
        0,
        file.size().toInt()
    )?.asImageBitmap()
}

actual suspend fun Clipboard.withPlainText(content: String) {
    this.setClipEntry(ClipEntry(ClipData.newPlainText("", content)))
}

/**
 * Converts any value to SHA-256 hash
 */
actual fun Any.toSha256(): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(toString().toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}

/** Retrieves the current device name */
actual fun deviceName(): String? {
    val context = getKoin().get<Context>()
    return Settings.Global.getString(context.contentResolver, "device_name")
}
