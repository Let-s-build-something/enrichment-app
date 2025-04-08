package components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
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
    val loadOriginal = rememberSaveable {
        mutableStateOf(false)
    }
    val displayOriginal = rememberSaveable {
        mutableStateOf(false)
    }

    val thumbnailRequest =  ImageRequest.Builder(platformContext)
        .data(if(displayOriginal.value) url else thumbnail)
        .crossfade(true)
        .listener(
            onSuccess = { _, _ ->
                loadOriginal.value = true
            }
        )
        .build()

    val originalRequest = ImageRequest.Builder(platformContext)
        .data(if(displayOriginal.value) url else thumbnail)
        .crossfade(true)
        .listener(
            onSuccess = { _, _ ->
                displayOriginal.value = true
            }
        )
        .build()

    AsyncImage(
        modifier = modifier,
        model = if(displayOriginal.value) originalRequest else thumbnailRequest,
        contentDescription = contentDescription,
        contentScale = contentScale
    )
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