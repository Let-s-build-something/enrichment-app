package base.utils

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import dev.gitlive.firebase.storage.Data
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readBytes
import org.jetbrains.skia.Image
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.DataFlavor.stringFlavor
import java.awt.datatransfer.Transferable
import java.security.MessageDigest

/** Returns a bitmap from a given file */
actual suspend fun getBitmapFromFile(file: PlatformFile): ImageBitmap? {
    return Image.makeFromEncoded(file.readBytes()).toComposeImageBitmap()
}

actual suspend fun Clipboard.withPlainText(content: String) {
    setClipEntry(
        ClipEntry(
            object: Transferable {
                override fun getTransferDataFlavors(): Array<DataFlavor> = arrayOf(stringFlavor)
                override fun isDataFlavorSupported(flavor: DataFlavor?): Boolean = true
                override fun getTransferData(flavor: DataFlavor?) = content
            }
        )
    )
}

/**
 * Converts any value to SHA-256 hash
 * @return the generated SHA-256 hash
 */
actual fun Any.toSha256(): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(toString().toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}

/** Retrieves the current device name */
actual fun deviceName(): String? {
    val env = System.getenv()
    return if (env.containsKey("COMPUTERNAME")) env["COMPUTERNAME"]
    else if (env.containsKey("HOSTNAME")) env["HOSTNAME"]
    else "Unknown Computer"
}

actual fun fromByteArrayToData(byteArray: ByteArray): Data {
    TODO("Not yet implemented")
}