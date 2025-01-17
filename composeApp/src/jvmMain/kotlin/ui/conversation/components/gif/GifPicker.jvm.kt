package ui.conversation.components.gif

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import io.kamel.core.Resource
import io.kamel.core.utils.cacheControl
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
private fun SimpleKamelImageBox(
    resource: @Composable (BoxScope.() -> Resource<Painter>),
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.Center,
    animationSpec: FiniteAnimationSpec<Float>? = null,
    onLoading: (@Composable BoxScope.(Float) -> Unit)? = null,
    onFailure: (@Composable BoxScope.(Throwable) -> Unit)? = null,
    onSuccess: @Composable BoxScope.(Painter) -> Unit,
) {
    Box(modifier, contentAlignment) {
        resource().let { resource ->
            if (animationSpec != null) {
                Crossfade(resource, animationSpec = animationSpec) { animatedResource ->
                    when (animatedResource) {
                        is Resource.Loading -> if (onLoading != null) onLoading(animatedResource.progress)
                        is Resource.Success -> onSuccess(animatedResource.value)
                        is Resource.Failure -> if (onFailure != null) onFailure(animatedResource.exception)
                    }
                }
            } else {
                when (resource) {
                    is Resource.Loading -> if (onLoading != null) onLoading(resource.progress)
                    is Resource.Success -> onSuccess(resource.value)
                    is Resource.Failure -> if (onFailure != null) onFailure(resource.exception)
                }
            }
        }
    }
}

@Composable
private fun WrapContentKamelImage(
    resource: @Composable (BoxScope.() -> Resource<Painter>),
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
    SimpleKamelImageBox(
        resource,
        modifier,
        contentAlignment,
        animationSpec,
        onLoading,
        onFailure,
        onSuccess,
    )
}
