package ui.conversation.components.link

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import augmy.interactive.shared.ui.theme.LocalTheme
import components.AsyncSvgImage
import org.koin.compose.viewmodel.koinViewModel
import ui.conversation.components.audio.MediaProcessorModel

/** Visualization of an url */
@Composable
fun LinkPreview(
    modifier: Modifier = Modifier,
    url: String,
    alignment: Alignment.Horizontal,
    imageSize: IntSize = IntSize(height = 200, width = 250),
    textBackground: Color = LocalTheme.current.colors.backgroundDark
) {
    val density = LocalDensity.current
    val processorModel: MediaProcessorModel = koinViewModel(key = url)
    val graphProtocol = processorModel.graphProtocol.collectAsState()

    LaunchedEffect(Unit) {
        if(graphProtocol.value == null) {
            processorModel.requestGraphProtocol(url = url)
        }
    }

    if(graphProtocol.value != null) {
        Column(
            modifier = modifier.background(color = textBackground),
            horizontalAlignment = alignment
        ) {
            graphProtocol.value?.imageUrl?.let { image ->
                AsyncSvgImage(
                    modifier = modifier
                        .height(height = (imageSize.height.takeIf { it != 0 } ?: 160).dp)
                        .then(if(imageSize.width.takeIf { it != 0 } != null) {
                            Modifier.width(imageSize.width.dp)
                        } else Modifier)
                        .wrapContentWidth(),
                    model = image,
                    contentScale = ContentScale.Crop
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

                if(graphProtocol.value?.isEmpty != true) {
                    Column(
                        modifier = Modifier.padding(end = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        AnimatedVisibility(graphProtocol.value?.title != null) {
                            Text(
                                text = graphProtocol.value?.title ?: "",
                                style = LocalTheme.current.styles.title,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        AnimatedVisibility(graphProtocol.value?.description != null) {
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
    }
}
