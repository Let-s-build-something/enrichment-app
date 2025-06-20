package base.utils

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readBytes
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import org.jetbrains.skia.Image
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH
import platform.UIKit.UIDevice


/** Returns a bitmap from a given file */
actual suspend fun getBitmapFromFile(file: PlatformFile): ImageBitmap? {
    return Image.makeFromEncoded(file.readBytes()).toComposeImageBitmap()
}

@OptIn(ExperimentalComposeUiApi::class)
actual suspend fun Clipboard.withPlainText(content: String) {
    setClipEntry(ClipEntry.withPlainText(content))
}

/**
 * Converts any value to SHA-256 hash
 * @return the generated SHA-256 hash
 */
@OptIn(ExperimentalForeignApi::class)
actual fun Any.toSha256(): String {
    return this.toString().toSha256()
}

@OptIn(ExperimentalForeignApi::class)
fun String.toSha256(): String {
    val data = encodeToByteArray()
    val digest = UByteArray(CC_SHA256_DIGEST_LENGTH)

    data.usePinned { pinnedData ->
        digest.usePinned { pinnedDigest ->
            CC_SHA256(
                pinnedData.addressOf(0),
                data.size.toUInt(),
                pinnedDigest.addressOf(0)
            )
        }
    }

    return digest.joinToString("") { byte ->
        byte.toInt().and(0xFF).toString(16).padStart(2, '0')
    }
}

/** Retrieves the current device name */
actual fun deviceName(): String? {
    return UIDevice.currentDevice.name
}
