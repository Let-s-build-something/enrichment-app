package ui.conversation.components.giphy

import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import io.ktor.http.Url

/** Image displaying a GIF from an [url] */
@Composable
actual fun GifImage(
    modifier: Modifier,
    url: String,
    contentDescription: String?,
    contentScale: ContentScale,
    onLoading: ((Float) -> Unit)?
) {
    val resource = asyncPainterResource(data = Url(url))

    KamelImage(
        modifier = modifier,
        resource = {
            resource
        },
        onLoading = { progress ->
            onLoading?.invoke(progress)
        },
        animationSpec = tween(),
        contentDescription = contentDescription,
        contentScale = contentScale
    )
}