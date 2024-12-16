package ui.conversation.components.gif

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import coil3.gif.GifDecoder
import coil3.request.ImageRequest
import coil3.request.crossfade
import components.platformContext

/** Image displaying a GIF from an [url] */
@Composable
actual fun GifImage(
    modifier: Modifier,
    url: String,
    contentDescription: String?,
    contentScale: ContentScale
) {
    AsyncImage(
        modifier = modifier,
        model = ImageRequest.Builder(platformContext)
            .data(url)
            .crossfade(true)
            .decoderFactory(GifDecoder.Factory())
            .build(),
        onSuccess = {
            onLoading?.invoke(1f)
        },
        contentDescription = contentDescription,
        contentScale = contentScale
    )
}