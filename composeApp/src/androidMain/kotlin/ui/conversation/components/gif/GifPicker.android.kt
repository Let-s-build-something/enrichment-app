package ui.conversation.components.gif

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import coil3.gif.GifDecoder
import coil3.request.ImageRequest
import coil3.request.crossfade
import components.platformContext
import io.github.vinceglb.filekit.core.PlatformFile

/** Image displaying a GIF from data */
@Composable
actual fun GifImage(
    modifier: Modifier,
    data: Any,
    contentDescription: String?,
    contentScale: ContentScale
) {
    val transformedData = remember(data) {
        mutableStateOf(data)
    }

    if(data is PlatformFile) {
        LaunchedEffect(Unit) {
            transformedData.value = data.readBytes()
        }
    }

    AsyncImage(
        modifier = modifier,
        model = ImageRequest.Builder(platformContext)
            .data(transformedData.value)
            .crossfade(true)
            .decoderFactory(GifDecoder.Factory())
            .build(),
        contentDescription = contentDescription,
        contentScale = contentScale
    )
}