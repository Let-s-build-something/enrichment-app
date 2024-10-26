package components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.svg.SvgDecoder
import data.Asset

@Composable
fun AsyncSvgImage(
    modifier: Modifier = Modifier,
    asset: Asset,
    contentDescription: String? = null
) {
    AsyncImage(
        modifier = modifier,
        model = ImageRequest.Builder(platformContext)
            .data(asset.url)
            .crossfade(true)
            .decoderFactory(SvgDecoder.Factory())
            .build(),
        contentDescription = contentDescription
    )
}