package base.utils

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import dev.gitlive.firebase.storage.Data
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import io.github.vinceglb.filekit.readBytes
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.usePinned
import org.jetbrains.skia.Image
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIDevice


/** Returns a bitmap from a given file */
actual suspend fun getBitmapFromFile(file: PlatformFile): ImageBitmap? {
    return Image.makeFromEncoded(file.readBytes()).toComposeImageBitmap()
}

/**
 * Converts any value to SHA-256 hash
 * @return the generated SHA-256 hash
 */
@OptIn(ExperimentalForeignApi::class)
actual fun sha256(value: Any?): String {
    memScoped {
        val data = value.toString().encodeToByteArray()
        val digest = UByteArray(CC_SHA256_DIGEST_LENGTH)

        data.usePinned { pinnedData ->
            digest.usePinned { pinnedDigest ->
                CC_SHA256(
                    data = pinnedData.addressOf(0),
                    len = data.size.toUInt(),
                    md = pinnedDigest.addressOf(0)
                )
            }
        }
        return digest.joinToString("") {
            it.toString(16).padStart(2, '0')
        }
    }
}

/** Retrieves the current device name */
actual fun deviceName(): String? {
    return UIDevice.currentDevice.name
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual fun fromByteArrayToData(byteArray: ByteArray): Data {
    return byteArray.usePinned { pinned ->
        Data(NSData.create(bytes = pinned.addressOf(0), length = byteArray.size.toULong()))
    }
}
