package ui.conversation.components.gif

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import io.kamel.core.Resource
import io.kamel.core.utils.cacheControl
import io.kamel.image.KamelImageBox
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

    WrapContentKamelImage(
        modifier = modifier,
        resource = { resource },
        onFailure = { it.printStackTrace() },
        contentDescription = contentDescription,
        contentScale = contentScale
    )
}

@Composable
private fun WrapContentKamelImage(
    resource: @Composable (BoxWithConstraintsScope.() -> Resource<Painter>),
    contentDescription: String?,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null,
    onLoading: (@Composable BoxScope.(Float) -> Unit)? = null,
    onFailure: (@Composable BoxScope.(Throwable) -> Unit)? = null,
    contentAlignment: Alignment = Alignment.Center,
    animationSpec: FiniteAnimationSpec<Float>? = null,
) {
    val onSuccess: @Composable (BoxScope.(Painter) -> Unit) = { painter ->
        Image(
            painter,
            contentDescription,
            modifier,
            alignment,
            contentScale,
            alpha,
            colorFilter
        )
    }
    KamelImageBox(
        resource,
        modifier,
        contentAlignment,
        animationSpec,
        onLoading,
        onFailure,
        onSuccess,
    )
}
