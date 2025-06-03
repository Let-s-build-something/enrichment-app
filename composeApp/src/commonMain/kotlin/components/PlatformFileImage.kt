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
import data.io.base.BaseResponse
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.name
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Visualized bitmap extracted from the ByteArray of a local file reference */
@Composable
fun PlatformFileImage(
    modifier: Modifier = Modifier,
    media: PlatformFile,
    contentScale: ContentScale = ContentScale.Fit,
    contentDescription: String? = null,
    onState: (BaseResponse<Any>) -> Unit
) {
    val bitmap = remember(media.name) {
        mutableStateOf<ImageBitmap?>(null)
    }

    LaunchedEffect(Unit) {
        if(bitmap.value == null) {
            withContext(Dispatchers.Default) {
                onState(BaseResponse.Loading)
                bitmap.value = getBitmapFromFile(media).also {
                    onState(
                        if(it == null) BaseResponse.Error() else BaseResponse.Success("")
                    )
                }
            }
        }
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