package components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import augmy.interactive.shared.ui.theme.LocalTheme
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter.State
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.svg.SvgDecoder

@Composable
fun AsyncSvgImage(
    modifier: Modifier = Modifier,
    model: Any?,
    onFinish: (() -> Unit)? = null,
    contentScale: ContentScale = ContentScale.Crop,
    contentDescription: String? = null
) {
    val state = remember(model) {
        mutableStateOf<State?>(null)
    }
    onFinish?.let {
        LaunchedEffect(state.value) {
            if(state.value is State.Success) it()
        }
    }

    Box(contentAlignment = Alignment.Center) {
        AsyncImage(
            modifier = modifier,
            model = ImageRequest.Builder(platformContext)
                .data(model)
                .crossfade(true)
                .decoderFactory(SvgDecoder.Factory())
                .build(),
            onState = {
                state.value = it
            },
            contentDescription = contentDescription,
            contentScale = contentScale
        )
        AnimatedVisibility(state.value is State.Loading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .zIndex(1f)
                    .requiredSize(32.dp),
                color = LocalTheme.current.colors.brandMainDark,
                trackColor = LocalTheme.current.colors.tetrial
            )
        }
    }
}