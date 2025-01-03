package ui.conversation.components.gif

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import io.kamel.core.utils.cacheControl
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import io.ktor.http.CacheControl
import io.ktor.http.Url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

/** Image displaying a GIF from data */
@Composable
actual fun GifImage(
    modifier: Modifier,
    data: Any,
    contentDescription: String?,
    contentScale: ContentScale
) {
    val resource = asyncPainterResource(
        key = data,
        data = if(data is String) Url(data) else data
    ) {
        coroutineContext = Job() + Dispatchers.IO
        requestBuilder {
            cacheControl(CacheControl.MaxAge(maxAgeSeconds = 86400))
        }
    }

    KamelImage(
        modifier = modifier,
        resource = { resource },
        onLoading = { progress ->
            //onLoading?.invoke(progress)
        },
        onFailure = { it.printStackTrace() },
        //animationSpec = tween(),
        contentDescription = contentDescription,
        contentScale = contentScale
    )
}