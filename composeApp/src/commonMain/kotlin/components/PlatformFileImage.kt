package components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import base.utils.getBitmapFromFile
import io.github.vinceglb.filekit.core.PlatformFile

/** Visualized bitmap extracted from the ByteArray of a local file reference */
@Composable
fun PlatformFileImage(
    modifier: Modifier = Modifier,
    media: PlatformFile,
    contentScale: ContentScale = ContentScale.Fit,
    contentDescription: String? = null
) {
    val bitmap = remember(media.name) {
        mutableStateOf<ImageBitmap?>(null)
    }

    LaunchedEffect(Unit) {
        bitmap.value = getBitmapFromFile(media)
    }

    bitmap.value?.let { value ->
        Image(
            modifier = modifier,
            bitmap = value,
            contentScale = contentScale,
            contentDescription = contentDescription
        )
    }
}