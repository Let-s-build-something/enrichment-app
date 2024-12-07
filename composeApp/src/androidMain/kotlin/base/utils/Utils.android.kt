package base.utils

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import io.github.vinceglb.filekit.core.PlatformFile

/** Returns a bitmap from a given file */
actual suspend fun getBitmapFromFile(file: PlatformFile): ImageBitmap? {
    return BitmapFactory.decodeByteArray(
        file.readBytes(),
        0,
        file.getSize()?.toInt() ?: 0
    )?.asImageBitmap()
}