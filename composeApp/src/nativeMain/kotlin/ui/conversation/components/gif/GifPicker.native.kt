package ui.conversation.components.gif

import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import io.ktor.http.Url

/** Image displaying a GIF from data */
@Composable
actual fun GifImage(
    modifier: Modifier,
    data: Any,
    contentDescription: String?,
    contentScale: ContentScale
) {
    val resource = asyncPainterResource(
        data = if(data is String) Url(data) else data
    )

    KamelImage(
        modifier = modifier,
        resource = {
            resource
        },
        animationSpec = tween(),
        contentDescription = contentDescription,
        contentScale = contentScale
    )
}