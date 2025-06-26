package ui.conversation.components.link

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import augmy.interactive.shared.ui.base.LocalScreenSize
import augmy.interactive.shared.ui.theme.LocalTheme
import components.AsyncSvgImage
import org.koin.compose.viewmodel.koinViewModel
import ui.conversation.components.audio.MediaProcessorModel

/** Visualization of an url */
@Composable
fun LinkPreview(
    modifier: Modifier = Modifier,
    url: String,
    shape: Shape = RectangleShape,
    alignment: Alignment.Horizontal,
    imageSize: IntSize = IntSize(height = 160, width = 0),
    onLayout: (isVisible: Boolean) -> Unit = {},
    textBackground: Color = LocalTheme.current.colors.backgroundDark
) {
    val density = LocalDensity.current
    val screenSize = LocalScreenSize.current
    val processorModel: MediaProcessorModel = koinViewModel(key = url)
    val graphProtocol = processorModel.graphProtocol.collectAsState()

    LaunchedEffect(Unit) {
        if(graphProtocol.value == null) {
            processorModel.requestGraphProtocol(url = url)
        }
    }

    onLayout(graphProtocol.value?.isEmpty == false)

    AnimatedVisibility(graphProtocol.value?.isEmpty == false) {
        Column(
            modifier = modifier
                .width(IntrinsicSize.Min)
                .background(color = textBackground, shape = shape)
                .heightIn(max = screenSize.height.times(.3f).dp),
            horizontalAlignment = alignment
        ) {
            graphProtocol.value?.imageUrl?.let { image ->
                AsyncSvgImage(
                    modifier = modifier
                        .align(Alignment.CenterHorizontally)
                        .sizeIn(
                            minHeight = imageSize.height.dp,
                            minWidth = 200.dp
                        )
                        .wrapContentWidth()
                        .clip(shape),
                    model = image,
                    contentScale = ContentScale.Fit
                )
            }
            Row(
                modifier = (if(graphProtocol.value?.imageUrl == null) modifier else Modifier)
                    .fillMaxWidth()
                    .then(if(graphProtocol.value?.isEmpty == false) Modifier.width(IntrinsicSize.Min) else Modifier)
                    .padding(vertical = 6.dp, horizontal = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Top
            ) {
                graphProtocol.value?.iconUrl?.let { icon ->
                    if(icon != graphProtocol.value?.imageUrl) {
                        AsyncSvgImage(
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .size(
                                    with(density) { LocalTheme.current.styles.regular.fontSize.toDp() * 3 - 4.dp}
                                ),
                            model = icon,
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                Column(
                    modifier = Modifier.padding(end = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = graphProtocol.value?.title ?: "",
                        style = LocalTheme.current.styles.title,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = graphProtocol.value?.description ?: "",
                        style = LocalTheme.current.styles.regular,
                        maxLines = 5,
                        minLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
