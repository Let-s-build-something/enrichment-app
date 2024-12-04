package base

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import io.github.vinceglb.filekit.core.PlatformFile
import org.jetbrains.skia.Image

actual suspend fun getBitmapFromFile(file: PlatformFile): ImageBitmap? {
    return Image.makeFromEncoded(file.readBytes()).toComposeImageBitmap()
}