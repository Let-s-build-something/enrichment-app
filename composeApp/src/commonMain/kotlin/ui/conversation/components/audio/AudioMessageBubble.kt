package ui.conversation.components.audio

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import augmy.interactive.shared.ui.base.LocalScreenSize
import augmy.interactive.shared.ui.theme.LocalTheme
import base.utils.audio.rememberAudioPlayer
import org.koin.compose.viewmodel.koinViewModel

/**
 * Message bubble that has embedded player and is automatically downloaded and cached via [url].
 */
@Composable
fun AudioMessageBubble(
    modifier: Modifier = Modifier,
    url: String,
    processorModel: AudioProcessorModel = koinViewModel()
) {
    val screenSize = LocalScreenSize.current
    val density = LocalDensity.current
    val headingFontSize = LocalTheme.current.styles.heading.fontSize

    val resultByteArray = processorModel.resultByteArray.collectAsState()

    val waveformHeight = remember {
        with(density) { headingFontSize.toDp() }
    }
    val expectedBarsCount = remember(screenSize.width) { ((screenSize.width - 32) / 8) }
    val player = rememberAudioPlayer(
        key = resultByteArray.value,
        byteArray = resultByteArray.value ?: ByteArray(0),
        secondsPerBar = 0.1,
        barsCount = expectedBarsCount
    )


    LaunchedEffect(url) {
        processorModel.downloadByteArray(url)
    }


    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        LazyRow(modifier = Modifier.animateContentSize()) {
            items(
                items = player.barPeakAmplitudes.value,
                key = { bar -> bar.second }
            ) { bar ->
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(waveformHeight)
                        .animateItem(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .heightIn(min = 6.dp, max = waveformHeight)
                            .width(4.dp)
                            .background(
                                color = LocalTheme.current.colors.secondary,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .height((bar.first / player.peakMedian.value * waveformHeight.value).dp)
                    )
                }
            }
        }
    }
}
