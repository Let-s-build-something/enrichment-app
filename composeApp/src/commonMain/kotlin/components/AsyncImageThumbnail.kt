package components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.PlatformContext
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import data.Asset

/** Async image intended for larger image loading with low resolution being loaded first */
@Composable
fun AsyncImageThumbnail(
    modifier: Modifier = Modifier,
    thumbnail: String,
    url: String,
    contentScale: ContentScale = ContentScale.Fit,
    contentDescription: String? = null
) {
    val isOriginalLoaded = rememberSaveable(url) { mutableStateOf(false) }

    val blurRadius = remember(url) { Animatable(6f) }
    val originalOpacity = remember(url) { Animatable(0f) }

    val thumbnailRequest = ImageRequest.Builder(platformContext)
        .data(thumbnail)
        .listener(
            onSuccess = { _, _ ->
                if (!isOriginalLoaded.value) {
                    isOriginalLoaded.value = true
                }
            }
        )
        .build()

    val originalRequest = ImageRequest.Builder(platformContext)
        .data(url)
        .crossfade(true)
        .listener(
            onSuccess = { _, _ ->
                isOriginalLoaded.value = true
            }
        )
        .build()

    LaunchedEffect(isOriginalLoaded.value) {
        if (isOriginalLoaded.value) {
            blurRadius.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 500)
            )
            originalOpacity.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 500)
            )
        }
    }

    Box {
        AsyncImage(
            modifier = modifier
                .matchParentSize()
                .blur(blurRadius.value.dp),
            model = thumbnailRequest,
            contentDescription = contentDescription,
            contentScale = contentScale
        )

        if (isOriginalLoaded.value) {
            AsyncImage(
                modifier = modifier
                    .matchParentSize()
                    .graphicsLayer {
                        alpha = originalOpacity.value
                    },
                model = originalRequest,
                contentDescription = contentDescription,
                contentScale = contentScale
            )
        }
    }
}

/** Async image intended for larger image loading with low resolution being loaded first */
@Composable
fun AsyncImageThumbnail(
    modifier: Modifier = Modifier,
    image: Asset.Image,
    contentScale: ContentScale = ContentScale.Fit,
    contentDescription: String? = null
) {
    AsyncImageThumbnail(
        modifier = modifier,
        thumbnail = image.thumbnail,
        url = image.url,
        contentScale = contentScale,
        contentDescription = contentDescription
    )
}

expect val platformContext: PlatformContext