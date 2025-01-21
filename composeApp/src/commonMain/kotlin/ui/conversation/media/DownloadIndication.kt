package ui.conversation.media

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.download_failure
import augmy.composeapp.generated.resources.download_loading
import augmy.composeapp.generated.resources.download_open_file
import augmy.composeapp.generated.resources.download_success
import augmy.interactive.shared.ext.scalingClickable
import augmy.interactive.shared.ui.base.PlatformType
import augmy.interactive.shared.ui.base.currentPlatform
import augmy.interactive.shared.ui.theme.LocalTheme
import augmy.interactive.shared.ui.theme.SharedColors
import base.utils.downloadFiles
import base.utils.openFile
import components.LoadingIndicator
import data.io.base.BaseResponse
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import ui.conversation.components.audio.MediaProcessorModel

data class DownloadIndicationState(
    val state: MutableState<BaseResponse<*>?>,
    val progress: MutableState<LongRange>,
    val items: MutableState<Int>,
    val item: MutableState<Int>
)

@Composable
fun rememberIndicationState(
    processor: MediaProcessorModel,
    response: BaseResponse<*>? = null,
    progress: LongRange = 0L..0L,
    items: Int = 1,
    item: Int = 1
): DownloadIndicationState {
    val coroutineScope = rememberCoroutineScope()
    val state = remember(processor) {
        DownloadIndicationState(
            state = mutableStateOf(response),
            progress = mutableStateOf(progress),
            items = mutableStateOf(items),
            item = mutableStateOf(item)
        )
    }

    LaunchedEffect(Unit) {
        processor.resultData.collectLatest {
            if(it.isNotEmpty()) {
                downloadFiles(data = it)
            }
            coroutineScope.coroutineContext.cancelChildren()
            coroutineScope.launch {
                if(it.isNotEmpty()) {
                    state.state.value = BaseResponse.Success(null)
                    delay(if(currentPlatform == PlatformType.Jvm) 8000 else 1000)
                }
                state.state.value = null
                processor.flush()
            }
        }
    }

    LaunchedEffect(Unit) {
        processor.downloadProgress.collectLatest {
            state.state.value = when {
                it == null -> null
                it.item != 0 || (it.progress?.first ?: 0) > 0 -> BaseResponse.Loading
                else -> null
            }
            it?.progress?.let { progress ->
                state.progress.value = progress
            }
            it?.items?.let { items ->
                state.items.value = items
            }
            it?.item?.let { item ->
                state.item.value = item
            }
        }
    }

    return state
}

@Composable
fun DownloadIndication(
    modifier: Modifier = Modifier,
    state: DownloadIndicationState,
    shape: Shape = RectangleShape
) {
    Box(
        modifier = modifier
            .animateContentSize(
                alignment = Alignment.Center,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
            .zIndex(50f)
    ) {
        if(state.state.value != null) {
            val color = animateColorAsState(
                targetValue = when(state.state.value) {
                    is BaseResponse.Error -> SharedColors.RED_ERROR
                    is BaseResponse.Success -> LocalTheme.current.colors.brandMainDark
                    else -> LocalTheme.current.colors.backgroundDark
                }
            )
            val textColor = if(state.state.value is BaseResponse.Loading) {
                LocalTheme.current.colors.secondary
            }else LocalTheme.current.colors.tetrial

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = color.value,
                        shape = shape
                    )
                    .padding(vertical = 8.dp, horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LoadingIndicator(
                        response = state.state.value
                    )
                    Text(
                        text = when(state.state.value) {
                            is BaseResponse.Loading -> stringResource(Res.string.download_loading) +
                                    " ${if(state.items.value > 1) state.item.value / state.items.value else ""}" +
                                    " ${bytesAsRelative(state.progress.value.first)}/${bytesAsRelative(state.progress.value.last)}"
                            is BaseResponse.Success -> stringResource(Res.string.download_success)
                            is BaseResponse.Error -> stringResource(Res.string.download_failure)
                            else -> ""
                        },
                        style = LocalTheme.current.styles.category.copy(color = textColor)
                    )
                }
                if(currentPlatform == PlatformType.Jvm) {
                    Text(
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .scalingClickable {
                                openFile(null)
                            },
                        text = stringResource(Res.string.download_open_file),
                        style = LocalTheme.current.styles.category.copy(color = textColor)
                    )
                }
            }
        }
    }
}

/** Formats number of bytes to readable form */
fun bytesAsRelative(bytes: Long): String {
    return when {
        bytes < 1024 * 1024 * 1024 -> "${kotlin.math.round(bytes / (1024f * 1024f))} MB"
        bytes < 1024 * 1024 -> "${kotlin.math.round(bytes / 1024f)} KB"
        else -> "$bytes B"
    }
}
